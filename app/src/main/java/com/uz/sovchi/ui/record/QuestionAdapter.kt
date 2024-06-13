package com.uz.sovchi.ui.record

import com.uz.sovchi.R
import com.uz.sovchi.databinding.QuestionItemBinding
import com.uz.sovchi.ui.base.BaseAdapter
import com.uz.sovchi.ui.base.EmptyDiffUtil

data class Question(val text: String)

class QuestionAdapter : BaseAdapter<Question,QuestionItemBinding>(R.layout.question_item,EmptyDiffUtil()) {

    override fun bind(holder: ViewHolder<*>, model: Question, pos: Int) {
        holder.binding.apply {
            (this as QuestionItemBinding)
            textView.text = model.text
        }
    }
}