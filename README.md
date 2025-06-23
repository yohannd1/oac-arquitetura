# oac-arquitetura

Repositório com código para o trabalho de OAC (Organização e Arquitetura
de Computadores), no semestre 2025.1.

Nosso grupo ficou com: Arquitetura B, Assembly 4.

![](arq_b.jpeg)

```
add %<regA> %<regB>        || RegB <- RegA + RegB
add <mem> %<regA>          || RegA <- memória[mem] + RegA
add %<regA> <mem>          || Memória[mem] <- RegA + memória[mem]
sub <regA> <regB>          || RegB <- RegA - RegB
sub <mem> %<regA>          || RegA <- memória[mem] - RegA
sub %<regA> <mem>          || memória[mem] <- RegA - memória[mem]
move <mem> %<regA>         || RegA <- memória[mem]
move %<regA> <mem>         || memória[mem] <- RegA
move %<regA> %<regB>       || RegB <- RegA
move imm %<regA>           || RegA <- immediate
inc %<regA>                || RegA ++
inc <mem>                  || memória[mem] ++
jmp <mem>                  || PC <- mem (desvio incondicional)
jn <mem>                   || se última operação<0 então PC <- mem (desvio condicional)
jz <mem>                   || se última operação=0 então PC <- mem (desvio condicional)
jnz <mem>                  || se última operação|=0 então PC <- mem (desvio condicional)
jeq %<regA> %<regB> <mem>  || se RegA==RegB então PC <- mem (desvio condicional)
jgt %<regA> %<regB> <mem>  || se RegA>RegB então PC <- mem (desvio condicional)
jlw %<regA> %<regB> <mem>  || se RegA<RegB então PC <- mem (desvio condicional)
call <mem>                 || PC <- mem ( (desvio incondicional) mas, antes de desviar, empilha o endereço de retorno (endereço da instrução imediatamente posterior ao call (push(PC++) )
ret                        || PC <- pop() (desvio incondicional)
```

Referência do trabalho (e base para o código): https://sites.google.com/site/alvarodegas/degas-home-page/acad%C3%AAmico/disciplinas/2025-1/organiza%C3%A7%C3%A3o-e-arquitetura-de-computadores

## formato do arquivo assembly

TODO: arrumar esse texto

```java
/*
 * An assembly program is always in the following template
 * <variables>
 * <commands>
 * Obs.
 * 		variables names are always started with alphabetical char
 * 	 	variables names must contains only alphabetical and numerical chars
 *      variables names never uses any command name
 * 		names ended with ":" identifies labels i.e. address in the memory
 * 		Commands are only that ones known in the architecture. No comments allowed
 *
 * 		The assembly file must have the extention .dsf
 * 		The executable file must have the extention .dxf
 */
```

## formato do arqu
