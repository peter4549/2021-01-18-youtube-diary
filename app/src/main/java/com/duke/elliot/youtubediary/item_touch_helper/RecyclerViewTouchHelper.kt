package com.duke.elliot.youtubediary.item_touch_helper

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.duke.elliot.youtubediary.databinding.ItemDateBinding
import com.duke.elliot.youtubediary.main.diaries.DiaryAdapter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private const val toLeft = 0
private const val toRight = 1

@SuppressLint("ClickableViewAccessibility")
abstract class RecyclerViewTouchHelper(
        context: Context,
        private val recyclerView: RecyclerView,
        private val leftButtonWidth: Int,
        private val rightButtonWidth: Int
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var itemTouchHelper: ItemTouchHelper
    private lateinit var removerQueue: Queue<Int>

    private var leftButtons: MutableList<UnderlayButton>? = null
    private val leftButtonBuffer: MutableMap<Int, MutableList<UnderlayButton>>

    private var rightButtons: MutableList<UnderlayButton>? = null
    private val rightButtonBuffer: MutableMap<Int, MutableList<UnderlayButton>>

    private var swipePosition = -1
    private var swipeThreshold = 0.5F

    private val positionSwipedMap = mutableMapOf<Int, Boolean>()

    abstract fun instantiateRightUnderlayButton(viewHolder: RecyclerView.ViewHolder,
                                                rightButtonBuffer: MutableList<UnderlayButton>)

    abstract fun instantiateLeftUnderlayButton(viewHolder: RecyclerView.ViewHolder,
                                               leftButtonBuffer: MutableList<UnderlayButton>)

    private val gestureListener = object
        : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

            rightButtons?.let { rightButtons ->
                for (button in rightButtons)
                    if (button.onClick(e.x, e.y))
                        break
            }

            leftButtons?.let { leftButtons ->
                for (button in leftButtons)
                    if (button.onClick(e.x, e.y))
                        break
            }

            return true
        }
    }

    private val onTouchListener = View.OnTouchListener { _, event ->
        if (swipePosition < 0)
            return@OnTouchListener false

        val swipeViewHolder = recyclerView.findViewHolderForAdapterPosition(swipePosition)

        val point = Point(event?.rawX!!.toInt(), event.rawY.toInt())
        val rect = Rect()

        val swipedItem = swipeViewHolder?.itemView
        swipedItem?.getGlobalVisibleRect(rect)

        if (event.action == MotionEvent.ACTION_DOWN ||
                event.action == MotionEvent.ACTION_UP ||
                event.action == MotionEvent.ACTION_MOVE) {
            if (rect.top < point.y && rect.bottom > point.y) {
                if (swipePosition > 0)
                    positionSwipedMap[swipePosition] = false

                gestureDetector.onTouchEvent(event)
            }else {
                for (positionSwiped in positionSwipedMap) {
                    if (positionSwiped.value)
                        removerQueue.add(positionSwiped.key)
                    positionSwipedMap[positionSwiped.key] = false
                }

                swipePosition = -1
                recoverSwipedItem()
            }
        }

        false
    }

    @Synchronized
    private fun recoverSwipedItem() {
        while (!removerQueue.isEmpty()) {
            val position = removerQueue.poll()?.toInt() ?: -1
            if (position > -1)
                recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    init {
        this.recyclerView.setOnTouchListener(onTouchListener)
        this.gestureDetector = GestureDetector(context, gestureListener)

        this.rightButtons = ArrayList()
        this.rightButtonBuffer = HashMap()

        this.leftButtons = ArrayList()
        this.leftButtonBuffer = HashMap()

        this.removerQueue = InLinkedList()

        attachRecyclerView()
    }

    class InLinkedList: LinkedList<Int> () {
        override fun contains(element: Int): Boolean {
            return false
        }

        override fun lastIndexOf(element: Int): Int {
            return element
        }

        override fun remove(element: Int): Boolean {
            return false
        }

        override fun indexOf(element: Int): Int {
            return element
        }

        override fun add(element: Int): Boolean {
            return if (contains(element)) false
            else super.add(element)
        }
    }


    private fun attachRecyclerView() {
        itemTouchHelper = ItemTouchHelper(this)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun getMovementFlags(recyclerView: RecyclerView,
                                  viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlags = ItemTouchHelper.DOWN or ItemTouchHelper.UP
        val swipeFlags = ItemTouchHelper.START or ItemTouchHelper.END

        /** When limiting the behavior in certain situations,
         *  you can do something like this: */
        // Prevent swipe if note is locked.
        // if ((recyclerView.adapter as NoteAdapter).selectedNote!!.isLocked)
        //    swipeFlags = 0

        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val position = viewHolder.adapterPosition

        positionSwipedMap[position] = true

        swipePosition = position

        if (rightButtonBuffer.containsKey(swipePosition))
            rightButtons = rightButtonBuffer[swipePosition]
        else
            rightButtons!!.clear()

        rightButtonBuffer.clear()

        if (leftButtonBuffer.containsKey(swipePosition)) leftButtons =
                leftButtonBuffer[swipePosition]
        else leftButtons!!.clear()

        leftButtonBuffer.clear()

        if (direction == ItemTouchHelper.START) {
            swipeThreshold = 0.5F * rightButtons!!.size.toFloat() * rightButtonWidth.toFloat()

        }
        else if (direction == ItemTouchHelper.END) {
            swipeThreshold = 0.5F * leftButtons!!.size.toFloat() * leftButtonWidth.toFloat()
        }
    }

    override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
    ) {
        val position = viewHolder.adapterPosition
        var translationX = dX
        val itemView = viewHolder.itemView

        if (position < 0) {
            swipePosition = position
            return
        }

        /** DateItem do not instantiate buttons. */
        if (viewHolder is DiaryAdapter.ViewHolder) {
            if (viewHolder.binding is ItemDateBinding)
                return
        }

        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            if (dX < 0) {
                var buffer: MutableList<UnderlayButton> = ArrayList()
                if (!rightButtonBuffer.containsKey(position)) {
                    instantiateRightUnderlayButton(viewHolder, buffer)
                    rightButtonBuffer[position] = buffer
                } else
                    buffer = rightButtonBuffer[position]!!

                translationX = dX * rightButtonWidth  / itemView.width
                drawButton(c, itemView, buffer, position, translationX, toLeft)
            } else if (dX > 0){
                var buffer: MutableList<UnderlayButton> = ArrayList()
                if (!leftButtonBuffer.containsKey(position)) {
                    instantiateLeftUnderlayButton(viewHolder, buffer)
                    leftButtonBuffer[position] = buffer
                } else buffer = leftButtonBuffer[position]!!

                translationX = dX * leftButtonWidth / itemView.width
                drawButton(c, itemView, buffer, position, translationX, toRight)
            }

            super.onChildDraw(c, recyclerView, viewHolder, translationX, dY, actionState, isCurrentlyActive)
        }
    }

    private fun drawButton(c: Canvas, itemView: View, buffer: MutableList<UnderlayButton>,
                           position: Int, translationX: Float, direction: Int) {
        var left = itemView.left.toFloat()
        var right = itemView.right.toFloat()
        var buttonWidth = 0F

        if (direction == toLeft) buttonWidth = -1 * translationX / buffer.size
        else if (direction == toRight) buttonWidth = translationX / buffer.size

        if (direction == toLeft) {
            for (button in buffer) {
                setButtonStatusIcon(button, position)
                left = right - buttonWidth
                button.onDraw(
                        c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()),
                        position
                )
                right = left
            }
        } else if (direction == toRight) {
            for (button in buffer) {
                right = left + buttonWidth
                button.onDraw(
                        c, RectF(left, itemView.top.toFloat(), right, itemView.bottom.toFloat()),
                        position
                )
                left = right
            }
        }
    }

    override fun isItemViewSwipeEnabled(): Boolean = true
    override fun isLongPressDragEnabled(): Boolean = true

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        // return swipeThreshold
        return 1F
        // 0.75F: You need to drag item by 75% of his width(or height) to dismiss,
        // default value is 0.5F
    }

    override fun getSwipeEscapeVelocity(defaultValue: Float): Float {
        return 10F * defaultValue
    }

    override fun getSwipeVelocityThreshold(defaultValue: Float): Float {
        return 10F * defaultValue
    }


    /** Change button icon. */
    private fun setButtonStatusIcon(button: UnderlayButton, position: Int) {
        // The icon changes even if without the else clause,
        // but this is the code added for a quick change.
        /*
        if (((recyclerView.adapter as NoteAdapter).getNoteByPosition(position).alarmTime != null)
            && button.id == MainActivity.Companion.UnderlayButtonIds.ALARM
        ) {
            button.imageResourceId = R.drawable.ic_alarm_off_white_24dp
        } else if (button.id == MainActivity.Companion.UnderlayButtonIds.ALARM)
            button.imageResourceId = R.drawable.ic_add_alarm_white_24dp

        if ((recyclerView.adapter as NoteAdapter).getNoteByPosition(position).isDone
            && button.id == MainActivity.Companion.UnderlayButtonIds.DONE
        ) {
            button.imageResourceId = R.drawable.ic_done_all_white_24dp
        } else if (button.id == MainActivity.Companion.UnderlayButtonIds.DONE)
            button.imageResourceId = R.drawable.ic_done_white_24dp

         */
    }
}