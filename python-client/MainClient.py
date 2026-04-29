from Client import Client  # corrigido: 'from ./Client' é sintaxe inválida em Python


def main():
    cliente = Client("127.0.0.1", 12345)
    cliente.conectar()

    if cliente.esta_conectado():
        # --- Handshake inicial (cada mensagem é uma linha única) ---
        id_linha = cliente.receber_linha()
        if id_linha and id_linha.startswith("__ID__"):
            meu_id = id_linha.replace("__ID__", "").strip()
            print(f"Você é o Cliente #{meu_id}")

        boas_vindas = cliente.receber_linha()
        if boas_vindas:
            print(f"[Servidor] {boas_vindas}")

        # --- Loop principal ---
        print("Digite suas mensagens (digite 'sair' para encerrar):")

        while True:
            mensagem = input("> ")
            if mensagem.lower() == "sair":
                break

            if cliente.enviar_mensagem(mensagem):
                # receber_resposta() lida com respostas de 1 ou múltiplas linhas
                resposta = cliente.receber_resposta()
                if resposta is not None:
                    print(f"[Servidor] {resposta}")
                else:
                    break
            else:
                break

        cliente.fechar()


if __name__ == "__main__":
    main()