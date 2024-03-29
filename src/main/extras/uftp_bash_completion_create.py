#!/usr/bin/env python3

from subprocess import Popen, PIPE, STDOUT

TEMPLATE = "uftp_bash_completion.template"
OUTPUT = "uftp_bash_completion.sh"
OUTPUT2 = "../package/distributions/Default/src/etc/bash_completion.d/unicore-uftp"

CMD = "uftp"

######################################################################

def find_commands():
    commands = []
    print ("Running UFTP to get the list of commands ... ")
    p = Popen([CMD], stdout=PIPE, stderr=STDOUT, encoding="UTF-8")
    p.wait()
    for line in p.stdout.readlines():
        if not line.startswith(" "):
            continue
        else:
            commands.append(line.split()[0])

    return commands


def find_options(command):
    options = []
    print ("Getting options for %s" % command)
    p = Popen([CMD, command, "-h"], stdout=PIPE, stderr=STDOUT, encoding="UTF-8")
    p.wait()
    for line in p.stdout.readlines():
        if not line.startswith(" -"):
            continue
        else:
            s = line.split()[0]
            options.append(s.split(",")[1])

    return options


######################################################################

with open(TEMPLATE) as f:
    output = f.read()
    
commands = sorted(find_commands())
global_opts = find_options("info")
global_opts.remove("--raw")
global_opts.sort()
case_body = ""


for command in commands:

    opts = find_options(command)
    opts = list(set(opts) - set(global_opts))
    opts.sort()
    s = '    %s)\n    opts="$global_opts %s"\n    ;;\n' % (command,
                                                           " ".join(opts))
    case_body += s


output = output % {"commands": " ".join(commands),
                   "global_opts": " ".join(global_opts),
                   "case_body": case_body}


with open(OUTPUT, "w") as f:
    f.write(output)

p = Popen(["cp", OUTPUT, OUTPUT2])
p.wait()
