package com.example.otakumaster.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

object CopyToClipboard{
    fun copyTextToClipboard(
        context: Context,
        text:String,
        showToast:Boolean=true,
        toast:String="已复制到剪切板"
    ){
        val clipboard=context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip=ClipData.newPlainText("text",text)
        clipboard.setPrimaryClip(clip)

        if (showToast){
            Toast.makeText(context,toast,Toast.LENGTH_SHORT).show()
        }
    }
}
