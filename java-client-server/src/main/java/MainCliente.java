import java.util.Scanner;

public class MainCliente {
    public static void main(String[] args) {
        // Instancia o cliente apontando para o IP do servidor (localhost) e a porta 12345
        Cliente cliente = new Cliente("127.0.0.1", 12345);

        // Tenta estabelecer a conexão
        cliente.conectar();

        if (cliente.estaConectado()) {
            Scanner teclado = new Scanner(System.in);
            System.out.println("Digite suas mensagens (digite 'sair' para encerrar):");

            while (true) {
                String linha = teclado.nextLine();
                if (linha.equalsIgnoreCase("sair")) {
                    break;
                }
                // Envia para o servidor
                cliente.enviarMensagem(linha);
            }

            teclado.close();
            cliente.fechar();
            System.out.println("Conexão encerrada.");
        }
    }
}