package com.createexe.exe.llm

import android.util.Log

class LlamaEngine {
    companion object {
        init {
            try {
                System.loadLibrary("exe_llm_engine")
                Log.i("LlamaEngine", "Native library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e("LlamaEngine", "Failed to load native library.", e)
            }
        }
    }

    private var nativeContextPtr: Long = 0L

    private external fun nativeLoadModel(modelPath: String): Long
    private external fun nativeGenerate(contextPtr: Long, prompt: String): String
    private external fun nativeRelease(contextPtr: Long)

    fun load(absolutePath: String): Boolean {
        nativeContextPtr = nativeLoadModel(absolutePath)
        return nativeContextPtr != 0L
    }

    fun generate(prompt: String): String {
        if (nativeContextPtr == 0L) return "[Engine Error: Model not loaded]"
        return nativeGenerate(nativeContextPtr, prompt)
    }

    fun release() {
        if (nativeContextPtr != 0L) {
            nativeRelease(nativeContextPtr)
            nativeContextPtr = 0L
        }
    }
}
