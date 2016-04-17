import org.jetbrains.kotlin.maven.ExecuteKotlinScriptMojo

val mojo = ExecuteKotlinScriptMojo.INSTANCE

mojo.getLog().info("kotlin build script accessing build info of ${mojo.project.artifactId} project")