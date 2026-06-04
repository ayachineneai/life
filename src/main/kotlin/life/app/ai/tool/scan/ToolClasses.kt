package life.app.ai.tool.scan

import io.github.classgraph.ClassGraph
import life.app.ai.tool.annotations.Tool
import kotlin.reflect.KClass

internal object ToolClasses {
    fun find(basePackage: String, classLoader: ClassLoader): List<KClass<*>> {
        return ClassGraph()
            .enableAnnotationInfo()
            .enableMethodInfo()
            .acceptPackages(basePackage)
            .overrideClassLoaders(classLoader)
            .scan()
            .use { scanResult ->
                scanResult
                    .getClassesWithMethodAnnotation(Tool::class.java.name)
                    .loadClasses()
                    .sortedBy { type -> type.name }
                    .map { type -> type.kotlin }
            }
    }

    fun defaultClassLoader(): ClassLoader {
        return Thread.currentThread().contextClassLoader ?: ToolClasses::class.java.classLoader
    }
}
