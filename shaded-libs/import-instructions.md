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

cd $REPLPP_REPO_ROOT
sbt clean stage
echo 'println("Hello!")' > target/simple.sc
./scala-repl-pp --script target/simple.sc
```
