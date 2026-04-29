import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

public class MainCliente {
    public static void main(String[] args) throws Exception {
        Cliente cliente = new Cliente("127.0.0.1", 12345);
        cliente.conectar();

        if (cliente.estaConectado()) {
            Socket socket = cliente.getSocket();

            final int[] meuId = {0};
            Thread tRecepcao = new Thread(() -> {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
                    String linha;
                    while ((linha = in.readLine()) != null) {
                        if (linha.startsWith("__ID__")) {
                            meuId[0] = Integer.parseInt(linha.replace("__ID__", "").trim());
                            System.out.println("Você é o Cliente #" + meuId[0]);
                        } else {
                            System.out.println("[Servidor] " + linha);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Conexão com o servidor encerrada.");
                }
            });
            tRecepcao.setDaemon(true);
            tRecepcao.start();

            Scanner teclado = new Scanner(System.in);
            System.out.println("Comandos disponíveis: PING, UPTIME, RAND, PALINDROMO <frase>, VERSION, HELP");

            while (true) {
                System.out.print("> [Cliente #" + meuId[0] + "]: ");
                String linha = teclado.nextLine();
                if (linha.equalsIgnoreCase("sair")) break;
                cliente.enviarMensagem(linha);
            }

            teclado.close();
            cliente.fechar();
            System.out.println("Conexão encerrada.");
        }
    }
}