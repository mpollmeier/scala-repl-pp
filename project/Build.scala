import sbt.*

object Build {

  def newProject(internalName: String, scalaVersion: String): Project =
    newProject(internalName, scalaVersion, internalName)

  def newProject(internalName: String, scalaVersion: String, moduleName: String): Project = {
    val internalId = s"${internalName}_${scalaVersion}".replaceAll("\\.", "")
    val baseDir = file(internalName)
    val projectName = s"${moduleName}_${scalaVersion}"

    Project(internalId, baseDir).settings(
      Keys.name := projectName,
      Keys.scalaVersion := scalaVersion,
      Compile/Keys.unmanagedSourceDirectories += (Compile/Keys.sourceDirectory).value / s"scala-$scalaVersion",
      Keys.target := Keys.target.value / scalaVersion,
    )
  }

}
