#============================================================
# Programa ejemplo de MIPS
#============================================================

.data
msg_pedir:     .asciiz "\nIngrese un numero entero: "
msg_mayor:     .asciiz "\nEl numero es mayor que 5."
msg_menor:     .asciiz "\nEl numero es menor o igual que 5."
msg_suma:      .asciiz "\nNumero + Numero = "
msg_resta:     .asciiz "\nNumero - Numero = "
msg_mult:      .asciiz "\nNumero * Numero = "
msg_div:       .asciiz "\nNumero / Numero = "
msg_par:       .asciiz "\nEl numero es PAR."
msg_impar:     .asciiz "\nEl numero es IMPAR."
msg_loop:      .asciiz "\nNumeros desde N hasta 1 (loop): "
msg_for:       .asciiz "\nNumeros desde N hasta 0 (for tipo Pascal): "
msg_final:     .asciiz "\n\nPrograma finalizado.\n"
salto_linea:   .asciiz "\n"

.text
.globl main

#============================================================
# main: solicita numero y llama a las subrutinas con jal
#============================================================
main:
    # Solicitar numero
    li $v0, 4     #load inmediate li
    la $a0, msg_pedir      #load address
    syscall

    # Leer numero entero
    li $v0, 5           #5 es el codigo de servicio para leer un numero entero
    syscall
    move $t0, $v0          # $t0 = numero ingresado

    # Llamadas a subrutinas
    jal comparar_con_5     #jal es jumpo and link
    jal suma
    jal resta
    jal multiplicacion
    jal division
    jal par_impar
    jal loop_numeros
    jal for_pascal

    # Mensaje final
    li $v0, 4
    la $a0, msg_final
    syscall

    # Salida del programa
    li $v0, 10
    syscall


#============================================================
# 1) Comparar con 5
#============================================================
comparar_con_5:
    li $t1, 5
    ble $t0, $t1, menor_igual
    li $v0, 4
    la $a0, msg_mayor
    syscall
    jr $ra

menor_igual:
    li $v0, 4
    la $a0, msg_menor
    syscall
    jr $ra


#============================================================
# 2) Sumar numero consigo mismo
#============================================================
suma:
    li $v0, 4
    la $a0, msg_suma
    syscall

    add $t2, $t0, $t0
    li $v0, 1
    move $a0, $t2
    syscall

    jr $ra


#============================================================
# 3) Resta del numero consigo mismo
#============================================================
resta:
    li $v0, 4
    la $a0, msg_resta
    syscall

    sub $t3, $t0, $t0
    li $v0, 1
    move $a0, $t3
    syscall

    jr $ra


#============================================================
# 4) Multiplicacion
#============================================================
multiplicacion:
    li $v0, 4
    la $a0, msg_mult
    syscall

    mul $t4, $t0, $t0
    li $v0, 1
    move $a0, $t4
    syscall

    jr $ra


#============================================================
# 5) Division
#============================================================
division:
    li $v0, 4
    la $a0, msg_div
    syscall

    beqz $t0, evitar_div_cero  # evitar division por cero
    div $t0, $t0
    mflo $t5         #move from LO
    li $v0, 1
    move $a0, $t5
    syscall
    jr $ra

evitar_div_cero:
    li $v0, 4
    la $a0, salto_linea
    syscall
    li $v0, 4
    la $a0, msg_menor
    syscall
    jr $ra


#============================================================
# 6) Par o impar
#============================================================
par_impar:
    li $t6, 2
    div $t0, $t6
    mfhi $t7       #move from HI

    beqz $t7, es_par
    li $v0, 4
    la $a0, msg_impar
    syscall
    jr $ra

es_par:
    li $v0, 4
    la $a0, msg_par
    syscall
    jr $ra


#============================================================
# 7) Loop desde N hasta 1
#============================================================
loop_numeros:
    li $v0, 4
    la $a0, msg_loop
    syscall

    move $t8, $t0   # contador

loop_inicio:
    blez $t8, fin_loop   #branch less equal to zero
    li $v0, 1
    move $a0, $t8
    syscall

    li $v0, 4
    la $a0, salto_linea
    syscall

    addi $t8, $t8, -1       #add inmediate
    j loop_inicio

fin_loop:
    jr $ra


#============================================================
# 8) For tipo Pascal (downto)
#============================================================
for_pascal:
    li $v0, 4
    la $a0, msg_for
    syscall

    move $t9, $t0

for_inicio:
    bltz $t9, fin_for
    li $v0, 1
    move $a0, $t9
    syscall

    li $v0, 4
    la $a0, salto_linea
    syscall

    addi $t9, $t9, -1
    j for_inicio

fin_for:
    jr $ra