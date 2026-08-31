# Point accumulo-env at the quickstart's Hadoop and ZooKeeper installs

Work for #3766

install.sh substitutes tserver options and JAVA_HOME into accumulo-env.sh, but the Accumulo 4 template no longer has the lines those seds matched, so they silently did nothing and the servers came up on the template's placeholder paths. I substituted the real Hadoop and ZooKeeper paths instead, and appended JAVA_HOME and PATH, since accumulo-cluster starts the servers over ssh where exports from the calling shell never reach them.

Split out per your review — the scan server cache sizing that used to be in here is #3877 now, so this one is just the path substitution and touches only install.sh.

The sed-against-a-vendor-template approach is the one the quickstart already used, but if Accumulo 4 has a supported config hook I would rather use that.
