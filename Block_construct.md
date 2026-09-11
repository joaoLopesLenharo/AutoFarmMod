# 🧱 Block_construct.me — Guia de Criação de Texturas para Blocos Personalizados no Hytale

> Este arquivo serve como guia de referência para criar texturas de bloco customizadas
> para o AutoFarm Mod. Leia do início ao fim antes de abrir qualquer programa.

---

## 📁 Estrutura de Texturas Deste Mod

As texturas dos blocos ficam em:
```
src/main/resources/Common/BlockTextures/AutoFarm/
├── autofarm_block_top.png      ← Topo e base do bloco AutoFarm
├── autofarm_block_side.png     ← Lados do bloco AutoFarm (N/S/L/O)
├── autotreefarm_block_top.png  ← Topo e base do bloco Auto Tree Farm
└── autotreefarm_block_side.png ← Lados do bloco Auto Tree Farm (N/S/L/O)
```

Estas texturas são **bundled** no JAR do mod — o jogo nunca vai falhar por textura
faltando porque elas já estão incluídas. Para atualizar, basta substituir os arquivos
PNG por suas próprias artes e recompilar com `.\gradlew.bat jar installMod`.

---

## 🖥️ Melhor Programa para Iniciantes

### 🥇 Recomendação Principal: **Aseprite** (pago, ~$20)
- Site: https://www.aseprite.org/
- É o **padrão da indústria** para pixel art de jogos
- Exporta direto em PNG com dimensões exatas
- Tem **onion skinning**, camadas, paletas de cores e animações
- Interface projetada especificamente para pixel art

### 🥈 Alternativa Gratuita: **LibreSprite**
- Site: https://libresprite.github.io/
- Fork gratuito e open-source do Aseprite
- Funcionalidades quase idênticas ao Aseprite
- Ideal para quem ainda está aprendendo antes de investir

### 🥉 Alternativa Gratuita (mais simples): **Paint.NET**
- Site: https://www.getpaint.net/
- Mais fácil para iniciantes absolutos
- Plugins disponíveis para pixel art
- Interface similar ao MS Paint mas com camadas e ferramentas avançadas

### ❌ Evite para pixel art
- **Adobe Photoshop**: pode funcionar mas não é feito para isso
- **GIMP**: funciona mas interface confusa para iniciantes
- **MS Paint**: sem camadas, sem zoom adequado

---

## 📐 Especificações Técnicas das Texturas

| Parâmetro | Valor Recomendado |
|-----------|-------------------|
| **Resolução** | `512 × 512 px` (padrão Hytale) |
| **Formato** | PNG (obrigatório — sem JPEG) |
| **Canal Alpha** | Suportado (transparência funciona) |
| **Modo de cor** | RGB ou RGBA |
| **Profundidade** | 8 bits por canal |

> ⚠️ **Importante:** O Hytale usa textura única por face. Você cria um PNG separado
> para o topo e outro para os lados. Não existe `UV mapping` manual como em engines 3D.

---

## 🎨 Processo Passo a Passo (Aseprite / LibreSprite)

### Passo 1 — Criar o arquivo
1. Abra o Aseprite
2. `File > New`
3. **Width**: `512`, **Height**: `512`
4. **Color Mode**: `RGB Color`
5. Clique em `OK`

### Passo 2 — Configurar o zoom e grade
1. `View > Zoom > Fit Screen` para ver o canvas completo
2. (Opcional) `View > Show > Grid` e configure em `16×16` para alinhar pixels

### Passo 3 — Desenhar a textura

**Para autofarm_block_side.png:**
- Fundo: placa de ferro escura (`#3A3A3A`)
- Bordas: barra de cobre (`#B87333`)
- Centro: engrenagem + enxada pixel art
- Luzes indicadoras verdes nos cantos (`#00FF66`)

**Para autofarm_block_top.png:**
- Fundo: metal verde-escuro (`#1A3A2A`)
- Centro: engrenagem de cobre girando
- Bordas: runas brilhantes (`#00FFCC`)

**Para autotreefarm_block_side.png:**
- Fundo: prancha de carvalho (`#8B6914`)
- Cantos: suportes de ferro (`#333333`)
- Centro: machado + círculo verde brilhante (`#00FF44`)
- Detalhes: vinhas e folhas nas bordas

**Para autotreefarm_block_top.png:**
- Fundo: corte transversal de tronco (`#5C3A1A`)
- Anéis de crescimento em âmbar (`#D4831A`)
- Centro: ícone de muda brilhante

### Passo 4 — Exportar
1. `File > Export As`
2. Salve como `.png`
3. Substitua o arquivo em `src/main/resources/Common/BlockTextures/AutoFarm/`
4. Recompile: `.\gradlew.bat jar installMod`

---

## ⚡ Sistema de Fallback Implementado

O mod usa um sistema de **detecção em runtime** para escolher entre textura
customizada e vanilla:

```
Caminho customizado (dentro do JAR do mod):
  Common/BlockTextures/AutoFarm/autofarm_block_side.png   ← PRIORITÁRIO

Se o arquivo NÃO existir no JAR, o validador do Hytale lança SEVERE e o mod
falha. Por isso, as texturas placeholder geradas por IA já estão incluídas.
```

### Como o fallback funciona na prática:

1. **Com texturas customizadas** (padrão atual): Os PNGs estão bundled no JAR.
   O Hytale carrega a textura do mod.

2. **Para trocar pela textura vanilla** (debug): Edite os campos `Textures` nos
   arquivos JSON para apontar para texturas vanilla validadas:
   ```json
   "Up": "BlockTextures/Wood_Trunk_Oak_Top.png"
   ```
   Texturas vanilla validadas disponíveis:
   - `BlockTextures/Metal_Iron_Ornate.png`
   - `BlockTextures/Metal_Copper_Decorative_Top.png`
   - `BlockTextures/Wood_Trunk_Oak_Full.png`
   - `BlockTextures/Wood_Trunk_Oak_Top.png`
   - `BlockTextures/Wood_Amber_Trunk_Full.png`

3. **Para criar textura totalmente nova**: Crie o PNG, coloque em
   `src/main/resources/Common/BlockTextures/AutoFarm/minha_textura.png`
   e referencie no JSON como `"BlockTextures/AutoFarm/minha_textura.png"`.

---

## 🔍 Verificando Texturas Disponíveis no Hytale

Para listar texturas válidas do jogo base (útil para fallbacks), execute no PowerShell:

```powershell
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead(
    "$env:APPDATA\Hytale\install\release\package\game\latest\Assets.zip"
)
$zip.Entries | Where-Object { $_.FullName -match "BlockTextures.*\.png" -and $_.FullName -notmatch "Cracks|AutoFarm" } |
    Select-Object FullName |
    ForEach-Object { $_.FullName.Replace("Common/", "") }
$zip.Dispose()
```

---

## 🛠️ Ferramentas Adicionais Úteis

| Ferramenta | Uso | Link |
|-----------|-----|------|
| **Lospec Palette List** | Paletas de cores curadas para pixel art | https://lospec.com/palette-list |
| **Piskel** | Editor pixel art online (gratuito, sem instalação) | https://www.piskel.com/ |
| **Pixelart.com** | Alternativa online simples | https://www.pixilart.com/ |

---

## 📦 Referência Rápida de Arquivos

| Arquivo JSON | Campo Textura | PNG do Mod |
|-------------|--------------|-----------|
| `autofarm_block.json` | `Textures[0].Up/Down` | `AutoFarm/autofarm_block_top.png` |
| `autofarm_block.json` | `Textures[0].North/South/East/West` | `AutoFarm/autofarm_block_side.png` |
| `autotreefarm_block.json` | `Textures[0].Up/Down` | `AutoFarm/autotreefarm_block_top.png` |
| `autotreefarm_block.json` | `Textures[0].North/South/East/West` | `AutoFarm/autotreefarm_block_side.png` |

---

*Gerado automaticamente pelo AutoFarm Mod — última atualização: 2026-09-10*
