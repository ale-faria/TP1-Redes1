# Chat em Rede — Redes de Computadores I (PUC Minas)

Aplicação cliente-servidor de chat implementada em Java, desenvolvida como
trabalho prático da disciplina de Redes de Computadores I.

---

## Pré-requisito: instalar o JDK

Baixe e instale o JDK em: https://adoptium.net (recomendado: JDK 17 ou superior)

Após instalar, abra o Prompt de Comando e confirme:
```cmd
java -version
javac -version
```

Ambos devem exibir a versão instalada. Se aparecer "não é reconhecido como comando",
reinicie o computador após a instalação.

---

## Estrutura do projeto

```
chat-redes\
├── src\
│   └── chat\
│       ├── common\
│       │   └── Message.java        # Objeto de mensagem (serializado via TCP)
│       ├── server\
│       │   ├── ChatServer.java     # Servidor: aceita TCP + responde UDP
│       │   └── ClientHandler.java  # Thread por cliente TCP conectado
│       └── client\
│           └── ChatClient.java     # Cliente com interface Swing
├── compile.bat
├── run_server.bat
├── run_client.bat
└── README.md
```

---

## Como compilar e executar

### 1. Compilar

Abra o Prompt de Comando dentro da pasta `chat-redes` e execute:
```cmd
compile.bat
```

Ou, se preferir rodar o comando diretamente:
```cmd
mkdir bin
javac -d bin -sourcepath src src\chat\common\Message.java src\chat\server\ChatServer.java src\chat\server\ClientHandler.java src\chat\client\ChatClient.java
```

### 2. Executar o servidor (no PC2 / máquina servidora)

Abra um Prompt de Comando e execute:
```cmd
run_server.bat
```

O servidor exibirá:
```
=== Servidor de Chat iniciado ===
TCP na porta: 12345
UDP na porta: 12346
Aguardando conexões TCP...
Aguardando pings UDP...
```

### 3. Executar o cliente (no PC1 / máquina cliente)

Abra outro Prompt de Comando e execute:
```cmd
run_client.bat
```

Uma janela Swing abrirá pedindo o IP do servidor e o nome do usuário.

Para testar na mesma máquina, use `127.0.0.1` como IP.
Para testar entre dois computadores na mesma rede, descubra o IP do servidor com:
```cmd
ipconfig
```
Use o valor de "Endereço IPv4" (ex: `192.168.0.10`).

---

## Requisitos atendidos

| Requisito           | Como é atendido                                                                      |
|---------------------|--------------------------------------------------------------------------------------|
| Java                | Todo o código é Java puro (JDK 8+)                                                   |
| Interface Swing     | ChatClient.java usa JFrame, JTextArea, JTextField, JButton, etc.                     |
| TCP                 | Mensagens de chat enviadas/recebidas via Socket / ServerSocket na porta 12345        |
| UDP                 | Pings de latência enviados a cada 2s via DatagramSocket na porta 12346               |
| Multithreading      | 3 threads no cliente (EDT + TCP Receiver + UDP Ping) + 1 thread por cliente no servidor |

### Por que TCP para o chat?
O chat exige entrega garantida e em ordem: uma mensagem perdida ou fora de
ordem quebraria a conversa. O TCP garante isso via handshake e retransmissão.

### Por que UDP para o ping de latência?
O ping é uma medição em tempo real onde perda não importa: se um pacote se
perder, simplesmente exibimos "timeout" e enviamos outro logo depois. O UDP tem
menor overhead e é a escolha natural para heartbeats e telemetria.

---

## Portas utilizadas

| Protocolo | Porta | Uso                       |
|-----------|-------|---------------------------|
| TCP       | 12345 | Mensagens de chat         |
| UDP       | 12346 | Ping/Pong de latência     |

Ao configurar o redirecionamento nos roteadores, redirecione ambas as portas
(12345 TCP e 12346 UDP) para o IP do servidor (PC2).

---

## Topologia de rede (com 2 roteadores)

```
PC1 ──(WiFi)── R1 ──(Cabo)── R2 ──(WiFi)── PC2
     192.168.x.x          172.16.x.x    10.x.x.x
```

Configuração de port forwarding necessária:
- R2: redirecionar portas 12345 e 12346 → IP de PC2
- R1: redirecionar portas 12345 e 12346 → IP de R2 (WAN)

---

## Firewall do Windows

Se o cliente não conseguir conectar ao servidor, verifique o Firewall do Windows.
Na máquina servidora, libere as portas 12345 (TCP) e 12346 (UDP):

1. Abra "Windows Defender Firewall com Segurança Avançada"
2. Clique em "Regras de Entrada" → "Nova Regra"
3. Selecione "Porta" → TCP → 12345 → Permitir a conexão
4. Repita o processo para UDP → 12346