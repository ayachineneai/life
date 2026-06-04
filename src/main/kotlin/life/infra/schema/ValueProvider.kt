package life.infra.schema

import com.fasterxml.classmate.ResolvedType
import com.github.victools.jsonschema.generator.CustomDefinition
import com.github.victools.jsonschema.generator.CustomDefinitionProviderV2
import com.github.victools.jsonschema.generator.SchemaGenerationContext
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import com.fasterxml.jackson.annotation.JsonValue as JacksonJsonValue

object ValueProvider : CustomDefinitionProviderV2 {
    override fun provideCustomSchemaDefinition(
        javaType: ResolvedType,
        context: SchemaGenerationContext,
    ): CustomDefinition? {
        val rawType = javaType.erasedType
        if (rawType.isEnum) {
            return null
        }

        val valueType = jsonValueType(rawType) ?: kotlinInlineValueType(rawType) ?: return null
        val resolvedValueType = context.typeContext.resolve(valueType)
        val definition = context.createStandardDefinition(resolvedValueType, this)
        return CustomDefinition(
            definition,
            CustomDefinition.DefinitionType.INLINE,
            CustomDefinition.AttributeInclusion.NO,
        )
    }

    private fun jsonValueType(type: Class<*>): Type? {
        val method = type.declaredMethods.singleOrNull { method ->
            method.isAnnotationPresent(JacksonJsonValue::class.java) &&
                method.parameterCount == 0 &&
                method.returnType != Void.TYPE
        }
        if (method != null) {
            return method.genericReturnType
        }

        return type.declaredFields.singleOrNull { field ->
            field.isAnnotationPresent(JacksonJsonValue::class.java) }?.genericType
    }

    private fun kotlinInlineValueType(type: Class<*>): Type? {
        if (!type.kotlin.isValue) {
            return null
        }

        return type.declaredFields.singleOrNull { field ->
            !field.isSynthetic && !Modifier.isStatic(field.modifiers)
        }?.genericType
    }
}
