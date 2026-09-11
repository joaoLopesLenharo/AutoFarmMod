# AutoFarm - Hytale Mod

Mod de automação agrícola e silvícola autônoma para **Hytale**. Introduz o bloco **AutoFarm**, projetado para automatizar o plantio, cultivo e colheita de sementes e mudas de árvores diretamente através de um baú conectado.

---

## 🌟 Funcionalidades

### 1. Conexão Direta com Baú Adjacente
- O baú **deve estar encostado** diretamente em uma das 6 faces do bloco AutoFarm ($\Delta x, \Delta y, \Delta z = 1$).
- Suporte a modo de **Detecção Automática (AUTO)** ou seleção de face fixa.
- Retira sementes e mudas para plantio e armazena colheitas e subprodutos de volta no baú.

### 2. Interface Gráfica Interativa (Tecla [F])
- **Abertura Nativa**: Mire no bloco e pressione **[F]** (ou clique com o botão direito).
- **Inspeção em Tempo Real**: Exibe o bloco ou item encostado em cada uma das 6 faces:
  - **Cima (+Y)**
  - **Baixo (-Y)**
  - **Norte (-Z)**
  - **Sul (+Z)**
  - **Leste (+X)**
  - **Oeste (-X)**
- **Configuração Fácil**:
  - Botão individual para selecionar a face ativa do baú.
  - Botão **Modo AUTO** para buscar automaticamente qualquer face encostada com baú.
  - Feedback visual do status de conexão e coordenadas do baú conectado.

### 3. Silvicultura e Maturação de Árvores por Dias de Jogo
- O cálculo de crescimento de mudas de árvores respeita o sistema de dias de mundo (`plantedDay`).
- Árvores só são colhidas em seu **último estágio de maturidade**.
- Drops completos: Madeira/Troncos, Gravetos, Seiva de Árvore, Fibras, Casca e Mudas para replantio contínuo.

### 4. Agricultura Completa
- Arado de solo e plantio inteligente de colheitas (Trigo, Cenoura, Batata, Milho, Tomate, etc.).
- Colheita automática de frutos e sementes maduras com chance de obter *Essência da Vida* (`Ingredient_Life_Essence`).
- Preservação da propriedade da fazenda: fazendas vizinhas não interferem nos blocos cultivados por outras fazendas.

---

## 🌲 Bloco Auto Tree Farm (Farm Dedicada de Árvores)

Para automação compacta e contínua de madeira sem necessidade de espaço aberto para copas de árvores:

- **Requisito de Solo**: Deve ser colocado diretamente sobre um **bloco de terra** (`Dirt`, `Grass_Dirt`, etc.) logo abaixo dele ($y - 1$).
- **Alimentação por Muda**: Coloque mudas de árvore (`Plant_Sapling_*`) no baú conectado. O bloco consome a muda e inicia o ciclo de cultivo.
- **Produção Periódica**: A cada ciclo de tempo configurado, o bloco gera uma porção dos drops correspondentes à espécie da árvore cultivada:
  - **Madeira/Troncos** da espécie da muda (`Wood_Trunk_*`)
  - **Gravetos** (`Ingredient_Stick`)
  - **Seiva** (`Ingredient_Tree_Sap`)
  - **Casca** (`Ingredient_Bark`)
  - **Fibras** (`Ingredient_Fibre`)
  - **Chance de Muda Extra** para ciclo sustentável
- **Interface [F]**: Pressione **[F]** no bloco para monitorar a presença de terra, muda ativa e configurar a face do baú.

---

## 🔨 Receitas de Fabricação (Crafting)

Fabricados na Bancada de Trabalho (**Workbench**):

### AutoFarm (Agricultura e Silvicultura de Campo)
| Ingrediente | Quantidade |
| :--- | :---: |
| Tronco de Madeira (`Wood_Trunk`) | 4 |
| Barra de Ferro (`Ingredient_Bar_Iron`) | 4 |
| Barra de Cobre (`Ingredient_Bar_Copper`) | 2 |
| Enxada de Ferro (`Tool_Hoe_Iron`) | 1 |

### Auto Tree Farm (Farm Compacta de Madeira)
| Ingrediente | Quantidade |
| :--- | :---: |
| Tronco de Madeira (`Wood_Trunk`) | 6 |
| Barra de Ferro (`Ingredient_Bar_Iron`) | 4 |
| Barra de Cobre (`Ingredient_Bar_Copper`) | 2 |
| Machado de Ferro (`Tool_Axe_Iron`) | 1 |

---

## ⚙️ Configuração (`config/autofarm.json`)

O mod gera automaticamente um arquivo de configuração personalizável:

```json
{
  "scanIntervalTicks": 40,
  "horizontalRange": 64,
  "verticalRange": 8,
  "waterProximityMax": 4,
  "maxActivePlantsPerFarm": 64,
  "harvestCooldownTicks": 10,
  "treeMaturityMinDays": 2.5
}
```

- `scanIntervalTicks`: Intervalo entre ciclos de escaneamento (~2 segundos a 20 TPS).
- `horizontalRange`: Raio horizontal de atuação do bloco (até 64 blocos).
- `verticalRange`: Alcance vertical de escaneamento ($\pm 8$ blocos).
- `treeMaturityMinDays`: Dias mínimos de jogo para corte de árvores caso não haja definições nos assets.

---

## 📦 Instalação e Compilação

### Requisitos
- **Java 25** (JDK)
- **Hytale** (versão de lançamento com servidor local)

### Compilar e Instalar
```powershell
# Executa testes unitários
.\gradlew.bat test

# Compila o JAR e copia diretamente para a pasta de Mods do Hytale
.\gradlew.bat installMod
```

O arquivo gerado é instalado em:
```
%APPDATA%/Hytale/UserData/Mods/AutoFarm-1.0.0.jar
```

---

## 🎮 Como Usar no Jogo

1. Coloque o bloco **AutoFarm** no chão.
2. Coloque um **Baú encostado** em uma das faces do bloco (ou acima/abaixo dele).
3. Insira sementes ou mudas de árvores dentro do baú.
4. Aponte a mira para o bloco e pressione **[F]** para abrir o menu de configuração.
5. Verifique a lista de faces para confirmar que o baú foi detectado e configure a face desejada (ou mantenha em **Modo AUTO**).
6. A fazenda começará a operar autonomamente!
