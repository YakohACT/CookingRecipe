package main.java.AI.Ollama;

/**
 * Alibaba Qwen 系モデル(Ollama 経由)
 */
public class QwenProvider extends OllamaProvider {

    @Override
    public String[] getAvailableModels() {
        return new String[]{
                "qwen3.5:4b",
                "qwen3.5:9b",
                "qwen3-vl:4b",
                "qwen3-vl:8b"
        };
    }
}
