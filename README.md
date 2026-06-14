# Chat em Rede — Redes de Computadores I (PUC Minas)

Aplicação cliente-servidor de chat implementada em Java, desenvolvida como
trabalho prático da disciplina de Redes de Computadores I.

---

## Estrutura do projeto

```
chat-redes/
├── src/
│   └── chat/
│       ├── common/
│       │   └── Message.java        # Objeto de mensagem (serializado via TCP)
│       ├── server/
│       │   ├── ChatServer.java     # Servidor: aceita TCP + responde UDP
│       │   └── ClientHandler.java  # Thread por cliente TCP conectado
│       └── client/
│           └── ChatClient.java     # Cliente com interface Swing
├── compile.sh
├── run_server.sh
└── run_client.sh
```

---

## Como compilar e executar

### 1. Compilar
```bash
chmod +x compile.sh run_server.sh run_client.sh
./compile.sh
```

### 2. Executar o servidor (no PC2 / máquina servidora)
```bash
./run_server.sh
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
```bash
./run_client.sh
```
Uma janela Swing abrirá pedindo o IP do servidor e o nome do usuário.

---

## Requisitos atendidos

| Requisito               | Como é atendido                                                                 |
|-------------------------|---------------------------------------------------------------------------------|
| **Java**                | Todo o código é Java puro (JDK 8+)                                              |
| **Interface Swing**     | `ChatClient.java` usa `JFrame`, `JTextArea`, `JTextField`, `JButton`, etc.     |
| **TCP**                 | Mensagens de chat enviadas/recebidas via `Socket` / `ServerSocket` na porta 12345 |
| **UDP**                 | Pings de latência enviados a cada 2s via `DatagramSocket` na porta 12346        |
| **Multithreading**      | 3 threads no cliente (EDT + TCP Receiver + UDP Ping) + 1 thread por cliente no servidor |

### Por que TCP para o chat?
O chat exige **entrega garantida e em ordem**: uma mensagem perdida ou fora de
ordem quebraria a conversa. O TCP garante isso via handshake e retransmissão.

### Por que UDP para o ping de latência?
O ping é uma medição em tempo real onde **perda não importa**: se um pacote se
perder, simplesmente exibimos "timeout" e enviamos outro logo depois. O UDP tem
menor overhead e é a escolha natural para heartbeats e telemetria.

---

## Portas utilizadas

| Protocolo | Porta | Uso                          |
|-----------|-------|------------------------------|
| TCP       | 12345 | Mensagens de chat             |
| UDP       | 12346 | Ping/Pong de latência         |

> Ao configurar o redirecionamento nos roteadores, redirecione **ambas as portas**
> (12345 TCP e 12346 UDP) para o IP do servidor (PC2).

---

## Topologia de rede (com 2 roteadores)

```
PC1 ──(WiFi)── R1 ──(Cabo)── R2 ──(WiFi)── PC2
     192.168.x.x          172.16.x.x    10.x.x.x
```

Configuração de port forwarding necessária:
- **R2**: redirecionar portas 12345 e 12346 → IP de PC2
- **R1**: redirecionar portas 12345 e 12346 → IP de R2 (WAN)
