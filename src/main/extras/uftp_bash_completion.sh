
_uftp()
{
  local cur prev commands global_opts opts
  COMPREPLY=()
  cur=`_get_cword`
  prev="${COMP_WORDS[COMP_CWORD-1]}"
  commands="authenticate checksum cp info issue-token ls mkdir rcp rm share sync"
  global_opts="--auth --client --group --help --identity --oidc-agent --oidc-server --password --user --verbose"


  # parsing for uftp command word (2nd word in commandline.
  # uftp <command> [OPTIONS] <args>)
  if [ $COMP_CWORD -eq 1 ]; then
    COMPREPLY=( $(compgen -W "${commands}" -- ${cur}) )
    return 0
  fi

  # looking for arguments matching to command
  case "${COMP_WORDS[1]}" in
    authenticate)
    opts="$global_opts --bandwith-limit --bytes --compress --encrypt --persistent --streams"
    ;;
    checksum)
    opts="$global_opts --algorithm --bytes --recurse"
    ;;
    cp)
    opts="$global_opts --archive --bandwith-limit --bytes --compress --encrypt --preserve --recurse --resume --show-performance --split-threshold --streams --threads"
    ;;
    info)
    opts="$global_opts --raw"
    ;;
    issue-token)
    opts="$global_opts --inspect --lifetime --limited --renewable"
    ;;
    ls)
    opts="$global_opts --human-readable"
    ;;
    mkdir)
    opts="$global_opts "
    ;;
    rcp)
    opts="$global_opts --bandwith-limit --bytes --compress --encrypt --one-time-password --server --streams"
    ;;
    rm)
    opts="$global_opts --quiet --recurse"
    ;;
    share)
    opts="$global_opts --access --delete --lifetime --list --one-time --server --write"
    ;;
    sync)
    opts="$global_opts --bandwith-limit --bytes --compress --encrypt --streams"
    ;;

  esac

  COMPREPLY=( $(compgen -W "${opts}" -- ${cur}) )
  
  _filedir

}

complete -o filenames -F _uftp uftp
