# Garimpo

Monitor de preços local. Você cadastra produtos de lojas online com um preço-alvo, o app consulta o preço a cada 6 horas e avisa quando ele atinge a meta.

![Tela do Garimpo](screenshots/garimpo-tela.png)

## Como funciona

- Coleta agendada com Jsoup, lendo o preço em diferentes formatos de página (JSON do Next.js, JSON-LD e meta tags) e tratando formatos de valor monetário.
- Histórico em H2/JPA com um registro por produto por dia (o menor preço visto), usado para calcular o preço médio e o menor preço dos últimos 90 dias.
- Notificação do sistema ao atingir o preço-alvo, sem repetir o aviso enquanto o preço continuar abaixo da meta.
- Intervalo de 3 segundos entre as requisições.

## Stack

Java 21, Spring Boot, Jsoup, JPA/Hibernate, H2, Maven.

## Como rodar

Requer JDK 21 ou superior.

```bash
mvn clean package
java -jar target/garimpo.jar
```

Depois abra http://localhost:8081. Também dá para rodar a classe `GarimpoApplication` direto pela IDE.

No meu uso pessoal, o backend sobe junto com o Windows e um atalho abre a interface em janela própria.

## Limitações

- Testado com Kabum, Gigantec, CiaPC, Pato Louco e Guerra Digital. Lojas com proteção anti-bot (como Pichau e Terabyte) bloqueiam a leitura automática e não são suportadas.
- Depende do HTML de cada loja: se a página mudar, a coleta daquela loja pode parar de funcionar.
- Média e menor preço só ficam úteis depois de alguns dias de coleta (a tela mostra "baseado em N dia(s) de dados").
- O preço lido é o que a loja publica nos dados da página, que pode ser o preço no PIX ou no cartão, dependendo da loja.
- Precisa do computador ligado para coletar. Feito para uso pessoal e testado apenas no Windows.