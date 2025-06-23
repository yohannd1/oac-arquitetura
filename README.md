# oac-arquitetura

Repositório com código para o trabalho de OAC (Organização e Arquitetura
de Computadores), no semestre 2025.1.

Nosso grupo ficou com: Arquitetura B, Assembly 4.

![](https://lh6.googleusercontent.com/Oe9nmaltMB7KBEQxLVH1MgslHcKZD38_VLoVhTgtMdqwsPr_8ZGDcA4N2H3y4PFo0WL_bsozBKFFkxMiuhd6wwWFEERfXt_c22xikGjH3n9r2EL0RvPDMwLenphVpHOf5r11tIYD_yd4BXIyHhI-V_WgN-0UaxMHRVa-MhCPfUvO7DdO9Y1P2g=w1280)

```
Microprograma: add %<regA> %<regB>        || RegB <- RegA + RegB
Microprograma: add <mem> %<regA>          || RegA <- memória[mem] + RegA
Microprograma: add %<regA> <mem>          || Memória[mem] <- RegA + memória[mem]
Microprograma: sub <regA> <regB>          || RegB <- RegA - RegB
Microprograma: sub <mem> %<regA>          || RegA <- memória[mem] - RegA
Microprograma: sub %<regA> <mem>          || memória[mem] <- RegA - memória[mem]
Microprograma: move <mem> %<regA>         || RegA <- memória[mem]
Microprograma: move %<regA> <mem>         || memória[mem] <- RegA
Microprograma: move %<regA> %<regB>       || RegB <- RegA
Microprograma: move imm %<regA>           || RegA <- immediate
Microprograma: inc %<regA>                || RegA ++
Microprograma: inc <mem>                  || memória[mem] ++
Microprograma: jmp <mem>                  || PC <- mem (desvio incondicional)
Microprograma: jn <mem>                   || se última operação<0 então PC <- mem (desvio condicional)
Microprograma: jz <mem>                   || se última operação=0 então PC <- mem (desvio condicional)
Microprograma: jnz <mem>                  || se última operação|=0 então PC <- mem (desvio condicional)
Microprograma: jeq %<regA> %<regB> <mem>  || se RegA==RegB então PC <- mem (desvio condicional)
Microprograma: jgt %<regA> %<regB> <mem>  || se RegA>RegB então PC <- mem (desvio condicional)
Microprograma: jlw %<regA> %<regB> <mem>  || se RegA<RegB então PC <- mem (desvio condicional)
Microprograma: call <mem>                 || PC <- mem ( (desvio incondicional) mas, antes de desviar, empilha o endereço de retorno (endereço da instrução imediatamente posterior ao call (push(PC++) )
Microprograma: ret                        || PC <- pop() (desvio incondicional)
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
