# Import instructions and context information for shaded libraries

## Why do we ship a shaded copy of other libraries and not use dependencies?
Scala-REPL-PP includes some small libraries that have been copied as-is, but then moved into the `replpp.shaded` namespace. We didn't include them as regular dependencies, because repl users may want to use a different version of them, which may be incompatible with the version the repl uses. Thankfully their license is very permissive - a big thanks to the original authors!

## os-lib: TODO

## geny: TODO

## scopt
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/scopt

cd /tmp
rm -rf scopt
git clone https://github.com/scopt/scopt.git
cd scopt
git checkout v4.1.0

rm -rf $TARGET
cp -rp shared/src/main/scala/scopt/ $TARGET
cp -rp jvm/src/main/scala/* $TARGET
cp -rp LICENSE.md $TARGET
sed -i 's/^package scopt$/package replpp.shaded.scopt/' $TARGET/*
sed -i 's/scopt.Read/Read/g' $TARGET/PlatformReadInstances.scala

cd $REPLPP_REPO_ROOT
sbt clean test
```

## mainargs
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/mainargs

cd /tmp
rm -rf mainargs
git clone https://github.com/com-lihaoyi/mainargs.git
cd mainargs
git checkout 0.5.1

rm -rf $TARGET
mkdir -p $TARGET
cp -rp LICENSE $TARGET
cp -rp mainargs/src/* $TARGET
cp -rp mainargs/src-jvm/* $TARGET
cp -rp mainargs/src-3/* $TARGET
sed -i 's/^package mainargs$/package replpp.shaded.mainargs/' $TARGET/*
sed -i '2iimport replpp.shaded.mainargs' $TARGET/Macros.scala $TARGET/TokensReader.scala
sed -i 's/requiredClass("mainargs./requiredClass("replpp.shaded.mainargs./' $TARGET/Macros.scala
sed -i 's/^import scala.collection.compat._$/import scala.collection.Factory/' $TARGET/TokensReader.scala

cd $REPLPP_REPO_ROOT
sbt clean stage
echo 'println("Hello!")' > target/simple.sc
./scala-repl-pp --script target/simple.sc
```

## sourcecode
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/sourcecode

cd /tmp
rm -rf sourcecode
git clone https://github.com/com-lihaoyi/sourcecode.git
cd sourcecode
git checkout 0.3.0

rm -rf $TARGET
mkdir -p $TARGET
cp -rp LICENSE $TARGET
cp -rp sourcecode/src/sourcecode/* $TARGET
cp -rp sourcecode/src-3/sourcecode/* $TARGET

sed -i 's/^package sourcecode$/package replpp.shaded.sourcecode/' $TARGET/*
sed -i '2iimport replpp.shaded.sourcecode' $TARGET/Macros.scala

cd $REPLPP_REPO_ROOT
sbt clean test
```

## fansi
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/fansi

cd /tmp
rm -rf fansi
git clone https://github.com/com-lihaoyi/fansi.git
cd fansi
git checkout 0.4.0

rm -rf $TARGET
mkdir -p $TARGET
cp -rp LICENSE $TARGET
cp -rp fansi/src/fansi/Fansi.scala $TARGET

sed -i 's/^package fansi$/package replpp.shaded.fansi/' $TARGET/*
sed -i '2iimport replpp.shaded.{fansi, sourcecode}' $TARGET/Fansi.scala

cd $REPLPP_REPO_ROOT
sbt clean test
```

## pprint
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/pprint

cd /tmp
rm -rf PPrint
git clone https://github.com/com-lihaoyi/PPrint.git
cd PPrint
git checkout 0.8.1

rm -rf $TARGET
mkdir -p $TARGET
cp -rp LICENSE $TARGET
cp -rp pprint/src/pprint/* $TARGET
cp -rp pprint/src-3/* $TARGET

sed -i '1ipackage replpp.shaded' $TARGET/package.scala
sed -i 's/^package pprint$/package replpp.shaded.pprint/' $TARGET/*.scala
sed -i '2iimport replpp.shaded.sourcecode' $TARGET/PPrinter.scala
sed -i 's/colorLiteral: fansi.Attrs/colorLiteral: replpp.shaded.fansi.Attrs/' $TARGET/PPrinter.scala
sed -i 's/colorApplyPrefix: fansi.Attrs/colorApplyPrefix: replpp.shaded.fansi.Attrs/' $TARGET/PPrinter.scala
sed -i '2iimport replpp.shaded.fansi' $TARGET/*.scala

cd $REPLPP_REPO_ROOT
sbt clean test
```

## coursier (only core.Version)
```
# start location must be replpp repo root!
REPLPP_REPO_ROOT=$(pwd)
TARGET_ROOT=${REPLPP_REPO_ROOT}/shaded-libs/src/main/scala/replpp/shaded/coursier/core
TARGET=${TARGET_ROOT}/Version.scala

cd /tmp
rm -rf coursier
git clone https://github.com/coursier/coursier.git
cd coursier
git checkout v2.1.5

rm -r $TARGET
mkdir -p $TARGET_ROOT
cp -p modules/core/shared/src/main/scala/coursier/core/Version.scala $TARGET

sed -i '1ipackage replpp.shaded' $TARGET
sed -i '/import coursier.core.compatibility._/d' $TARGET
sed -i '/import dataclass.data/d' $TARGET
sed -i '/import scala.collection.compat.immutable.LazyList/d' $TARGET
sed -i 's/@data /case /g' $TARGET

# insert "RichChar" from coursier.core.compatibility
sed -i '/^object Version {$/a\
\
  implicit class RichChar(val c: Char) extends AnyVal {\
    private def between(c: Char, lower: Char, upper: Char) = lower <= c && c <= upper\
\
    def letterOrDigit: Boolean =\
      between(c, __0__, __9__) || letter\
    def letter: Boolean = between(c, __a__, __z__) || between(c, __A__, __Z__)\
  }' $TARGET
# i didn't find a better way to escape the single quotes within that sed above...
sed -i "s/__/'/g" $TARGET

cd $REPLPP_REPO_ROOT
sbt clean test
```
