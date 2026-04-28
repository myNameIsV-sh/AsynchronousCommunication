import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

    private final int porta;
    private ServerSocket serverSocket;
    private final AtomicInteger contadorId = new AtomicInteger(0);
    private static final int MAX_CONEXOES = 5;
    private final Semaphore semaforo = new Semaphore(MAX_CONEXOES);
    private volatile boolean encerrando = false;

    private final ConcurrentHashMap<Integer, SessaoCliente> sessoesAtivas = new ConcurrentHashMap<>();

    public Servidor(int porta) {
        this.porta = porta;
    }

    public void iniciar() {
        // Inicia a thread administrativa para monitorar o console do servidor
        monitorarComandos();

        try {
            serverSocket = new ServerSocket(porta);
            System.out.println("Servidor iniciado na porta " + porta);
            System.out.println("Aguardando conexões (Limite: " + MAX_CONEXOES + ")...");

            while (!serverSocket.isClosed()) {
                try {
                    Socket cliente = serverSocket.accept();
                    String enderecoCliente = cliente.getInetAddress().getHostAddress();

                    // Tenta adquirir uma vaga no semáforo sem bloquear a thread principal
                    if (semaforo.tryAcquire()) {
                        int idCliente = contadorId.incrementAndGet();
                        atenderCliente(idCliente, cliente);
                    } else {
                        System.out.println("Limite de conexões atingido para: " + enderecoCliente);
                        fecharSocketImediatamente(cliente);
                    }
                } catch (SocketException e) {
                    if (serverSocket.isClosed()) {
                        System.out.println("Servidor parou de aceitar novas conexões.");
                    } else {
                        System.err.println("Erro no accept: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro fatal no servidor: " + e.getMessage());
        }
    }

    private void atenderCliente(int idCliente, Socket cliente) {
        BlockingQueue<String> filaEcho      = new LinkedBlockingQueue<>();
        BlockingQueue<String> filaBroadcast = new LinkedBlockingQueue<>();

        SessaoCliente sessao = new SessaoCliente(cliente, filaEcho, filaBroadcast);
        sessoesAtivas.put(idCliente, sessao); // registro antes de iniciar as threads

        Runnable callbackFechamento = () -> encerrarConexao(idCliente, cliente);

        Thread tLeitura = new Thread(new ThreadLeitura(idCliente, cliente, filaEcho, callbackFechamento, this));
        Thread tEscrita = new Thread(new ThreadEscrita(idCliente, cliente, filaEcho, filaBroadcast));

        tLeitura.start();
        tEscrita.start();
    }

    private void monitorarComandos() {
        Thread adminThread = new Thread(() -> {
            Scanner console = new Scanner(System.in);
            System.out.println("[ADMIN] Digite 'sair' a qualquer momento para encerrar o servidor.");
            while (true) {
                if (console.hasNextLine()) {
                    String comando = console.nextLine();
                    if (comando.equalsIgnoreCase("sair") || comando.equalsIgnoreCase("encerrar")) {
                        fechar();
                        break;
                    }
                    realizarBroadcast("[Servidor]: " + comando, -1);
                }
            }
        });
        adminThread.setDaemon(true); // Garante que a thread feche se o servidor parar
        adminThread.start();
    }

    // Método sincronizado para garantir que o encerramento ocorra apenas uma vez por cliente
    private synchronized void encerrarConexao(int idCliente, Socket cliente) {
        try {
            if (cliente != null && !cliente.isClosed()) {
                cliente.close();
                if (!encerrando) {
                    semaforo.release();         // só libera vaga em desconexão normal
                    sessoesAtivas.remove(idCliente);
                    System.out.println("[Servidor] Cliente #" + idCliente + " desconectado. Vaga liberada.");
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao encerrar conexão do cliente #" + idCliente + ": " + e.getMessage());
        }
    }

    public void realizarBroadcast(String msg, int idRemetente) {
        for (Map.Entry<Integer, SessaoCliente> entrada : sessoesAtivas.entrySet()) {
            if (entrada.getKey().equals(idRemetente)) continue; // não reenvia ao próprio remetente
            entrada.getValue().filaBroadcast.offer(msg);
        }
    }

    private void fecharSocketImediatamente(Socket cliente) {
        try {
            cliente.close();
        } catch (IOException e) {
            // Ignora erro ao fechar conexão recusada
        }
    }

    public void fechar() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                encerrando = true;
                System.out.println("[Servidor] Iniciando encerramento...");

                // 1. Para de aceitar novas conexões
                serverSocket.close();

                // 2. Percorre todas as sessões ativas e encerra cada uma
                for (Map.Entry<Integer, SessaoCliente> entrada : sessoesAtivas.entrySet()) {
                    int id = entrada.getKey();
                    SessaoCliente sessao = entrada.getValue();

                    try {
                        // Avisa a ThreadEscrita para encerrar limpa
                        sessao.filaEcho.offer(ThreadLeitura.POISON_PILL);

                        // Fecha o socket, o que interrompe a ThreadLeitura bloqueada no hasNextLine()
                        if (!sessao.socket.isClosed()) {
                            sessao.socket.close();
                        }

                        System.out.println("[Servidor] Sessão do Cliente #" + id + " encerrada.");
                    } catch (IOException e) {
                        System.err.println("[Servidor] Erro ao encerrar Cliente #" + id + ": " + e.getMessage());
                    }
                }

                sessoesAtivas.clear();
                System.out.println("[Servidor] Encerramento concluído.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar o servidor: " + e.getMessage());
        }
    }
}