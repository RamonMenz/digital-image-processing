# Sistema Base de Processamento Digital de Imagens

Sistema desktop desenvolvido em **Java** com **Swing** para a disciplina de **Processamento Digital de Imagens**. O sistema permite carregar, transformar e visualizar imagens utilizando diversos algoritmos implementados manualmente.

## 🔧 Requisitos

- **Java 21** ou superior
- **Maven 3.6+** (para build)

## 🚀 Como Executar

### Usando Maven

```bash
# Compilar o projeto
mvn clean compile

# Executar a aplicação
mvn exec:java "-Dexec.mainClass=br.feevale.App"

# Ou criar o JAR e executar
mvn clean package
java -jar target/digital-image-processing-1.0-SNAPSHOT.jar
```

### Usando IDE

1. Importe o projeto como **Maven Project** na sua IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Execute a classe `br.feevale.App`

### Descrição das Camadas

| Camada | Descrição |
|--------|-----------|
| **UI** | Interface gráfica com Swing. Responsável pela interação com o usuário. |
| **Core** | Lógica de processamento de imagens. Contém os algoritmos implementados. |
| **Model** | Representa os dados da aplicação (imagem original e transformada). |
| **Utils** | Utilitários para manipulação de pixels e operações de convolução. |

## 🔬 Detalhes de Implementação

### Manipulação de Pixels

O sistema utiliza `BufferedImage` do Java com acesso direto aos pixels através de `getRGB()` e `setRGB()`. Para melhor performance, os pixels são lidos em batch:

```java
// Lê todos os pixels de uma vez
int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);

// Processa os pixels
for (int i = 0; i < pixels.length; i++) {
    // Extrai componentes
    int a = (pixels[i] >> 24) & 0xFF;
    int r = (pixels[i] >> 16) & 0xFF;
    int g = (pixels[i] >> 8) & 0xFF;
    int b = pixels[i] & 0xFF;
    
    // Processa...
}

// Escreve todos os pixels de uma vez
result.setRGB(0, 0, width, height, pixels, 0, width);
```

## 📝 Notas

- Os algoritmos foram implementados manualmente para fins didáticos
- O sistema suporta imagens PNG, JPG e BMP
- A interface utiliza tema escuro para melhor visualização das imagens
- Transformações são aplicadas sempre sobre a imagem original, permitindo comparação lado a lado
