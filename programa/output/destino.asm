.data
_s1_: .word 0
_b1_: .word 0
numero: .word 0
letra: .byte 0
numS2: .word 0
t1: .word 0
t2: .word 0
t3: .word 0
numS: .word 0
f4: .float 0.0
floatNum: .float 0.0
_x22_: .byte 0
_miChar_: .byte 0
_miChar2_: .byte 0
t5: .word 0
_x30_: .word 0
_x40_: .word 0
var: .float 0.0
x24: .float 0.0
_x50_: .space 1000
_x51_: .word 0
str_lit_0: .asciiz "Hola a todos los que est[a] haciendo un compilador nuevo¿n"
t6: .word 0
str_lit_1: .asciiz "6 <= 45"
t7: .float 0.0
str_lit_2: .asciiz "var > 5.6"
t8: .word 0
str_lit_3: .asciiz "t6 && t7"
y: .word 0
x22: .byte 0
ch33: .byte 0
t9: .word 0
str_lit_4: .asciiz "x24 >= var"
t10: .float 0.0
t11: .word 0
str_lit_5: .asciiz "t9 && t10"
algo: .word 0
str2: .word 0
str_lit_6: .asciiz "sdff"
_i_: .word 0
str_lit_7: .asciiz "Hola mundo"
anotherNum: .word 0
miChar: .byte 0
miChar2: .byte 0
str1: .word 0
str_lit_8: .asciiz "Mi string 1"
fl1: .word 0
fl6: .float 0.0
fl4: .word 0
in1: .word 0
t12: .word 0
t13: .word 0
t14: .word 0
t15: .word 0
t16: .word 0
str_lit_9: .asciiz "3 ^ fl1"
t17: .word 0
fl2: .word 0
i: .word 0
arreglo: .space 4000
t18: .word 0
str_lit_10: .asciiz "4 ^ 5"
t19: .float 0.0
str_lit_11: .asciiz "6.7 != 8.9"
bl: .word 0
t20: .word 0
str_lit_12: .asciiz "true != false"
bl0: .word 0
t21: .word 0
str_lit_13: .asciiz "in1 >= fl1"
t22: .word 0
str_lit_14: .asciiz "t21 || false"
t23: .word 0
t24: .word 0
str_lit_15: .asciiz "! t23"
t25: .word 0
str_lit_16: .asciiz "t22 && t24"
bl1: .word 0

.text
.globl main
main:
# instrucción no traducida: FUNC_BEGIN func1 RET FLOAT
INICIO_funcion_func1:
# instrucción no traducida: Parametros_funcion_func1: [CHAR_x22]
li $t0, 10
sw $t0, numero
# asignación literal no soportada: letra = a
lw $a0, numero
li $v0, 1
syscall
lw $a0, letra
li $v0, 11
syscall
li $t0, 1
sw $t0, numS2
li $t0, 1
lw $t1, numero
mul $t2, $t0, $t1
sw $t2, t1
lw $t0, numero
lw $t1, t1
subu $t2, $t0, $t1
sw $t2, t2
lw $t0, t2
lw $t1, numero
addu $t2, $t0, $t1
sw $t2, t3
lw $t0, t3
sw $t0, numS
lw $a0, numS
li $v0, 1
syscall
# asignación literal no soportada: f4 = -0.01
lw $t0, f4
sw $t0, floatNum
# asignación literal no soportada: _x22_ = a
# asignación literal no soportada: _miChar_ = !
# asignación literal no soportada: _miChar2_ = !!
li $t0, -1
sw $t0, t5
lw $t0, t5
sw $t0, _x30_
li $t0, 0
sw $t0, _x40_
# asignación literal no soportada: var = 1.2
# asignación literal no soportada: x24 = 2.1
la $t0, _x50_
li $t1, 0
li $t3, 1
mul $t1, $t1, $t3
addu $t0, $t0, $t1
li $t2, 4
sw $t2, 0($t0)
la $t0, _x50_
li $t1, 1
li $t3, 1
mul $t1, $t1, $t3
addu $t0, $t0, $t1
li $t2, 5
sw $t2, 0($t0)
la $t0, _x50_
li $t1, 2
li $t2, 1000
arr_pad_0:
beq $t1, $t2, arr_pad_0_end
sw $zero, 0($t0)
addi $t0, $t0, 1
addi $t1, $t1, 1
j arr_pad_0
arr_pad_0_end:
la $t0, str_lit_0
sw $t0, _x51_
decide_begin_1:
la $t0, str_lit_1
sw $t0, t6
la $t0, str_lit_2
sw $t0, t7
la $t0, str_lit_3
sw $t0, t8
lw $t0, t8
bnez $t0, case_hit_3
j case_next_4
case_hit_3:
# asignación literal no soportada: x22 = b
# asignación literal no soportada: ch33 = a
j decide_end_2
case_next_4:
la $t0, str_lit_4
sw $t0, t9
la $t0, str_lit_2
sw $t0, t10
la $t0, str_lit_5
sw $t0, t11
lw $t0, t11
bnez $t0, case_hit_5
j case_next_6
case_hit_5:
# asignación literal no soportada: x22 = z
j decide_end_2
case_next_6:
la $t0, str_lit_6
sw $t0, str2
j decide_end_2
decide_end_2:
li $t0, 1
sw $t0, _i_
li $t0, 0
sw $t0, _i_
for_cond_7:
lw $t0, _i_
bnez $t0, 10
for_body_8:
lw $a0, _i_
li $v0, 1
syscall
lw $t0, _i_
li $t1, 1
addu $t2, $t0, $t1
sw $t2, _i_
j for_cond_7
for_end_9:
la $a0, str_lit_7
li $v0, 4
syscall
li $v0, 5
syscall
sw $v0, _x22_
lw $v0, var
jr $ra
FIN_funcion_func1:
# instrucción no traducida: FUNC_END func1
# instrucción no traducida: FUNC_BEGIN _func2_ RET BOOL
INICIO_funcion__func2_:
# instrucción no traducida: Parametros_funcion__func2_: [BOOL__b1_, INT__i1_]
li $t0, 1
sw $t0, anotherNum
li $v0, 1
jr $ra
FIN_funcion__func2_:
# instrucción no traducida: FUNC_END _func2_
# instrucción no traducida: FUNC_BEGIN _func3_ RET STRING
INICIO_funcion__func3_:
lw $v0, _b1_
jr $ra
FIN_funcion__func3_:
# instrucción no traducida: FUNC_END _func3_
# instrucción no traducida: FUNC_BEGIN notRincipal RET VOID
INICIO_funcion_notRincipal:
# asignación literal no soportada: miChar = !
# asignación literal no soportada: miChar2 = !!
la $t0, str_lit_8
sw $t0, str1
li $t0, 2
sw $t0, fl1
# asignación literal no soportada: fl6 = 56.6
li $t0, 3
sw $t0, fl4
li $t0, 2
sw $t0, in1
li $t0, 14
lw $t1, in1
mul $t2, $t0, $t1
sw $t2, t12
lw $t0, fl4
lw $t1, t12
subu $t2, $t0, $t1
sw $t2, t13
li $t0, 7
li $t1, 15
div $t0, $t1
mflo $t2
sw $t2, t14
lw $t0, t13
lw $t1, t14
addu $t2, $t0, $t1
sw $t2, t15
lw $t0, t15
sw $t0, in1
la $t0, str_lit_9
sw $t0, t16
lw $t0, t16
li $t1, 45
addu $t2, $t0, $t1
sw $t2, t17
lw $t0, t17
sw $t0, fl2
li $t0, 3
sw $t0, i
la $t0, arreglo
li $t1, 0
li $t3, 4
mul $t1, $t1, $t3
addu $t0, $t0, $t1
li $t2, 4
sw $t2, 0($t0)
la $t0, arreglo
li $t1, 1
li $t3, 4
mul $t1, $t1, $t3
addu $t0, $t0, $t1
li $t2, 5
sw $t2, 0($t0)
la $t0, arreglo
li $t1, 2
li $t2, 1000
arr_pad_1:
beq $t1, $t2, arr_pad_1_end
sw $zero, 0($t0)
addi $t0, $t0, 4
addi $t1, $t1, 1
j arr_pad_1
arr_pad_1_end:
la $t0, str_lit_10
sw $t0, t18
lw $t0, t18
sw $t0, fl1
la $t0, str_lit_11
sw $t0, t19
lw $t0, t19
sw $t0, bl
la $t0, str_lit_12
sw $t0, t20
lw $t0, t20
sw $t0, bl0
la $t0, str_lit_13
sw $t0, t21
la $t0, str_lit_14
sw $t0, t22
li $t0, 0
addiu $sp, $sp, -4
sw $t0, 0($sp)
lw $t0, in1
addiu $sp, $sp, -4
sw $t0, 0($sp)
jal _func2_
addiu $sp, $sp, 8
sw $v0, t23
la $t0, str_lit_15
sw $t0, t24
la $t0, str_lit_16
sw $t0, t25
lw $t0, t25
sw $t0, bl1
jr $ra
FIN_funcion_notRincipal:
# instrucción no traducida: FUNC_END notRincipal
