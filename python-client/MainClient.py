from Client import Client

def main():
    cliente = Client("127.0.0.1", 12345)
    cliente.conectar()

    if cliente.esta_conectado():
        print("Digite suas mensagens (digite 'sair' para encerrar):")
        
        while True:
            mensagem = input("> ")
            if mensagem.lower() == 'sair':
                break
            
            if cliente.enviar_mensagem(mensagem):
                resposta = cliente.receber_resposta()
                if resposta:
                    print(f"Servidor: {resposta}")
                else:
                    break
            else:
                break

        cliente.fechar()

if __name__ == "__main__":
    main()