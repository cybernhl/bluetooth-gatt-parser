package org.sputnikdev.bluetooth.gattparser.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import org.sputnikdev.bluetooth.gattparser.R

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var messageTextView: TextView
    private lateinit var runTestsButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 UI 元件
        messageTextView = view.findViewById(R.id.message)
        runTestsButton = view.findViewById(R.id.run_tests_button)

        // 設定按鈕點擊監聽器
        runTestsButton.setOnClickListener {
            messageTextView.text = "Running validation..."
            viewModel.runGattParserValidation()
        }

        // 觀察 ViewModel 中的 LiveData，當資料變更時更新 UI
        viewModel.validationResults.observe(viewLifecycleOwner) { resultText ->
            messageTextView.text = resultText
        }
    }
}
