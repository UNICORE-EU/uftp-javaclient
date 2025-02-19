Change log for the UFTP commandline client
==========================================

The issue tracker is at
https://github.com/UNICORE-EU/uftp-javaclient/issues


UFTP Client 2.2.0 (released Feb 14, 2025)
-----------------------------------------
 - improvement: "cp --resume" implementation now uses checksums in chunked mode
   to decide whether to transfer data
 - fix: re-use of sessions in "uftp ls" with multiple arguments did not work correctly
 - new feature: 'auth': option "--persistent" to create a persistent session
   (e.g. needed for curlftpfs)
 - updated third party dependencies

UFTP Client 2.1.0 (released Dec 11, 2024)
-----------------------------------------
 - new feature: add option to get an OAuth authentication token
   from an OIDC server such as Keycloak
 - fix: uploading multiple files to the default directory results
   in permission errors
 - show asserted UID (if any) in 'issue-token --inspect'
 - version updates

UFTP Client 2.0.0 (released Sep 20, 2024)
-----------------------------------------
 - new feature: re-written progress and performance display
   for 'cp -D'
 - fix: ssh-agent support not working on Mac (arm64)
 - version updates, code cleanup

UFTP Client 1.9.2 (released Jul 22, 2024)
-----------------------------------------
 - fix: 'cp': don't try to set modification time without
   "--preserve" option
 - fix: only warn if setting modification time fails
 - fix: remove unneeded slf4j jars from release

UFTP Client 1.9.1 (released May 14, 2024)
-----------------------------------------
 - fix: 'ls': change modification date format to resemble
   Unix 'ls -l'
 - fix: 'ls -H' option was triggering help output, and did
   not work as advertised
 - fix: 'cp' upload to default directory ("https://...:")

UFTP Client 1.9.0 (released Mar 06, 2024)
-----------------------------------------
 - new feature: 'issue-token' command for getting a
   JWT authentication token from the Auth server
 - new feature: additional option of AES encryption
   (to enable, set environment variable UFTP_ENCRYPTION_ALGORITHM="AES")
 - improvement: first try to get a required password from
   environment (UFTP_PASSWORD) before asking interactively
 - improvement: use new encryption key for every new session
 - improvement: add missing transfer options (multi-stream, encrypt,
   compress) to 'rcp'
 - improvement: command help output now groups the available
   options for better readability
 - fix: missing library caused oidc-agent authentication to fail

UFTP Client 1.8.2 (released Jan 09, 2024)
-----------------------------------------
 - fix: multi-chunk download of file only writes
   single chunk

UFTP Client 1.8.1 (released Dec 20, 2023)
-----------------------------------------
 - fix: missing library led to annoying "SLF4J" messages
 - improvement: use maximum keylength of 56 bytes for the
   Blowfish data encryption

UFTP Client 1.8.0 (released Nov 06, 2023)
-----------------------------------------
 - new feature: 'info' shows rate limit, session limit and reservations, if they exist
 - 'cp': more verbose output for chunked operations (with "-v")
 - 'cp': new option "-D" to switch on display of transfer performance
 - fix: 'rm': use RMD command to delete a directory
 - internal code refactoring and clean-up

UFTP Client 1.7.0 (released Jul 4, 2023)
----------------------------------------
 - by default, do NOT split up files. User must explicitely
   set a split threshold to activate this.
 - more output in --verbose mode
 - remove 'tunnel' command as it is no longer supported by uftpd
 - dependency updates

UFTP Client 1.6.1 (released Apr 17, 2023)
----------------------------------------
 - fix: error when querying password for encrypted SSH key

UFTP Client 1.6.0 (released Apr 12, 2023)
----------------------------------------
 - new feature: "uftp rcp" command for triggering
   server-to-server transfer (requires UFTPD 3.2.0 or later)
 - fix: improve anonymous access to shared files using the uftp protocol
   (i.e. using "uftp ls" and "uftp cp")
 - fix: when using "-u anonymous" as user, the UFTP client will not
   add ANY authentication
 - fix: use correct base directory for 'auth'

UFTP Client 1.5.0 (released Nov 7, 2022)
----------------------------------------

**JAVA VERSION NOTE** This release requires Java 11 or later!

 - documentation at https://uftp-docs.readthedocs.io
 - update to UNICORE 9.0 libs
 - 'share': add options for one-time and limited lifetime shares
 - 'info' : show server version and improve status output
 - fix: running "checksum" using wildcards resulted in
   creation of local "dummy" directories

UFTP Client 1.4.4 (released Feb 03, 2022)
----------------------------------------
 - fix: when running on Windows, remote paths
   erroneously contained '\' characters
 - fix: better error messages in case of missing arguments
 - improvement: for 'rm' command, add '-r' option that
   will also delete (sub-)directories

UFTP Client 1.4.3 (released Dec 15, 2021)
----------------------------------------
 - update to latest UNICORE base libs
 - new feature: 'checksum' command for getting
   hashes (MD5, SHA-1, SHA-256, SHA-512) for remote
   files from server (requires UFTPD 3.1.1 or later!)
 - fix: 'info' command: handle anonymous user correctly for
   UNICORE Storage endpoints
 - fix: always show https access link for new shares

UFTP Client 1.4.2 (released Aug 19, 2021)
----------------------------------------
 - improvement: add configurable write buffer size for
   filesystem writes
 - fix: set file modification time (MFMT) used wrong
   time of day
 - fix: Windows: uftp.bat was not working, update docs,
   handle more types of PuTTY keys
 - improvement: try to extract authserver's error message
   in case of auth failure
 - update dependencies

UFTP Client 1.4.1 (released Apr 8, 2021)
----------------------------------------
 - fix: upload in "resume" mode (cp -R) fails when target file
   doesn't exist
 - fix: correctly handle server's multi-stream limit of "1"

UFTP Client 1.4.0 (released Mar 5, 2021)
----------------------------------------
 - new feature: "autheticate" command for only authenticating
   and printing connect info
 - update to log4j2
 - commands can be abbreviated, e.g. "i" for "info"
 - 'cp' command: byte range can now ba simplified to just a single
   number of bytes to transfer, e.g. "uftp cp -B 10G ..."
 - fix: debian package now depends on "default-jre-headless"

UFTP Client 1.3.2 (released Sep 18, 2020)
-----------------------------------------
 - new feature: support FTP proxies (DeleGate, Frox)
 - fix: check ~/.uftp before ~/.ssh for private keys

UFTP Client 1.3.1 (released Jul 21, 2020)
-----------------------------------------
 - fix: don't query password for password-less keys 
 - fix: SSH: make sure key specified by "--identity ..." is used
   when ssh-agent is present

UFTP Client 1.3.0 (released Jun 17, 2020)
 -----------------------------------------
 - SSH: support for ed25519 keys
 - SSH: improved ssh-agent support when multiple keys
   are available

UFTP Client 1.2.0 (released Apr 16, 2020)
-----------------------------------------
 - improved error messages and removed stack traces
 - fix: client can hang when multiple threads are
   used (#60)


UFTP Client 1.1.0 (released Feb 13, 2020)
-----------------------------------------
 - improved "share" command
   (thanks to Jens Henrik Goebbert for many suggestions)
   - add separate option for server URL. If not given, the
   server URL is read from environment variable UFTP_SHARE_URL
   - non-absolute paths will be made absolute
   - --anonymous is the default sharing mode
 - console output for new shares now prints the http access URL
   and the full share info in verbose mode
 - if no auth option is given, use "-u $USER" as default
 - renamed 'get'/'put' to 'get-share'/'put-share'
 - fix: truncate existing local file when downloading to avoid
   corrupted file by only partially overwriting existing data (#56)

UFTP Client 1.0.0 (released Sep 30, 2019)
-----------------------------------------
 - new feature: client can create session using a UNICORE/X
   storage URL. This allows to use the client with
   any UNICORE installation that supports UFTP (#55)
 - new feature: 'cp' supports "archive" mode (2.7+ server)
   to unpack tar/zip streams on the server (#53)
 - new feature: support oidc-agent for authentication (#52)
 - new feature: allow to limit bandwidth per FTP connection
   ('-K <limit>', e.g. '-K 100M' limits to 100MB/sec)
 - new feature: deb and rpm packages available
 - ls: nicer output, added '-H' option for more human-readable 
   file size printout
 - fix: client should not try to open files in rw mode for upload
 - fix: error trying to set file mod time on "/dev/..." files
 - fix: local target directories are not created
 - fix: show correct file size for large files
 - fix: 'cp' recursion behaviour should match Unix 'cp'
 - remove non-functional buffer size option

UFTP Client 0.9.1 (released Aug 27, 2019)
-----------------------------------------
 - improved progress output in '-v' mode
 - cleaner handling of UFTPD server rejecting new client
   connections

UFTP Client 0.9.0 (released May 28, 2019)
-----------------------------------------
 - new feature: multiple client threads to
   speed up transfer of many files or large files in
   high-performance networks. Option "-t <n>" selects
   number of threads, "-T <nnn>" determines the minimum
   size of files that will be split up.

UFTP Client 0.8.0 (released Feb 4, 2019)
----------------------------------------
 - update to latest uftp-core and UNICORE base versions
 - new option "-i" to select SSH identity file (#49)

UFTP Client 0.7.0 (released  Jul 19, 2017)
------------------------------------------
 - new feature: data sharing support
   'share', 'get', 'put' commands (#33)

UFTP Client 0.6.0 (released  Nov 25, 2016)
------------------------------------------
 - new feature: 'mkdir' command
 - new feature: 'rm' command
 - fix: accept multiple sources for cp
   (https://sourceforge.net/p/unicore/uftp-issues/23)
 - make 'cp' behave more like Unix 'cp'
 - new feature: 'cp' can preserve file modification times
   (https://sourceforge.net/p/unicore/uftp-issues/28)
 - new feature: '-g' option to pass requested group name
   to auth server (and thus uftpd)
   (https://sourceforge.net/p/unicore/uftp-issues/31)
 - new feature: windows executable 'uftp.bat'
   (https://sourceforge.net/p/unicore/uftp-issues/18)

UFTP Client 0.5.0 (released June 14, 2016)
------------------------------------------
 - new feature: simpler URL scheme without username/password.
 Username/password given via "-u" option
 - new feature: support for different authorization headers,
 e.g. OIDC Bearer, via "-A" option
 - fix: better SSH agent support

UFTP Client 0.4.0 (released Jan 26, 2015)
----------------------------------------

 - new feature: "-r" option in "cp" command 
   attempts to resume a prior transfer
 - new feature: "info" command

UFTP Client 0.3.0 (released Nov 6, 2014)
----------------------------------------

 - fix: agent support
 - fix: handling remote paths
 - new feature: encryption and compression support 
   (requires at least UFTPD 2.2.0 and authserver 1.1.0)

UFTP Client 0.2.0 (released Oct 2, 2014)
----------------------------------------

 - new feature: "cp" can read from / write to console 
   streams, indicated by using "-" as file name
 - improvement: html/pdf manual with more extensive 
   documentation

UFTP Client 0.1.0 (released Sept 19, 2014)
------------------------------------------

First release

