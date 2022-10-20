package replpp

object ScriptRunner {
  def exec(config: Config): Unit = {
    val scriptFile = config.scriptFile.getOrElse(throw new AssertionError("scriptFile not defined"))
    if (!os.exists(scriptFile)) {
      throw new AssertionError(s"given script file $scriptFile does not exist")
    }

    System.err.println(s"executing $scriptFile with params=${config.params}")
    val scriptArgs: Seq[String] = {
//      val commandArgs = config.command.toList
      val parameterArgs = config.params.flatMap { case (key, value) => Seq(s"--$key", value) }
//      commandArgs ++ parameterArgs
      parameterArgs
    }

    // Our predef code includes import statements... I didn't find a nice way to add them to the context of the
    // script file, so instead we'll just write it to the beginning of the script file.
    // That's obviously suboptimal, e.g. because it messes with the line numbers.
    // Therefor, we'll display the temp script file name to the user and not delete it, in case the script errors.
    val predefCode = predefPlus(additionalImportCode(config) ++ importCpgCode(config))
    val predefPlusScriptFileTmp = Files.createTempFile("joern-script-with-predef", ".sc")
    val scriptCode = os.read(scriptFile)
    val scriptContent = wrapForMainargs(predefCode, scriptCode)
    if (config.verbose) println(scriptContent)
    Files.writeString(predefPlusScriptFileTmp, scriptContent)

    try {
      new ScriptingDriver(
        compilerArgs = compilerArgs(maybeAddDependencies(scriptCode, config)) :+ "-nowarn",
        scriptFile = predefPlusScriptFileTmp.toFile,
        scriptArgs = scriptArgs.toArray
      ).compileAndRun()

      // if the script failed: don't delete the temporary file which includes the predef,
      // so that the line numbers are accurate and the user can properly debug
      predefPlusScriptFileTmp.toFile.delete()
      System.err.println(s"script finished successfully")
    } catch {
      case t: Throwable =>
        if (isEncryptedScript) {
          /* minimizing exposure time by deleting the decrypted script straight away */
          scrr.toIO.delete()
          predefPlusScriptFileTmp.toFile.delete()
        }
        throw t
    }
  }
}
