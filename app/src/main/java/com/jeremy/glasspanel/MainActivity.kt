package com.jeremy.glasspanel

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Simple programmatically generated layout to verify installation
        val textView = TextView(this).apply {
            text = "GlassPanel Module Active\n\nTarget: SystemUI (Notification Panel)\nFramework: LibXposed API 102"
            textSize = 18f
            setPadding(64, 64, 64, 64)
        }
        
        setContentView(textView)
    }
}
