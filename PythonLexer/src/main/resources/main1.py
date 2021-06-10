
    # The first nonempty line
0b100110111     3     79228162514264337593543950336     0o377    0xdeadbeef     00j     00000
0067      0b110475        # wrong literals

"first'string'"'second"string\twith tab"''''third
multiline string
with empty single quote string next to it'''''
"this string has no closing quote
'this one too
"this one has backslash at the end\"
"another \\ example \t with \k backslashes"

23.133
893e+132
893e+hka   # wrong floating point literal (split into integer, id, plus, id)
0e0
000e0
00e-0
.e234  1.22e+32.23    # other wrong floating point literals
.
0.
.0
23.31e10
23.13e+10


# Only one NEWLINE token is generated in case of consecutive empty lines
# or lines with comments


aaa = 2+\bbb=3
    aaa //= 2+\          # backslash does not continue the line due to the comment
         3
bbb = 2**\
  3                     # explicit line continuation, no NEWLINE token is generated
  aaa = 4
    nnn = 4
   ggg = 4              # wrong indentation
  fff = 4

'''some
text\
here                    # error token: closing quotes are absent