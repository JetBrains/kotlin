package org.jetbrains.android.inspections.klint

import com.android.builder.model.AndroidProject
import com.android.builder.model.SourceProvider
import com.android.builder.model.Variant
import com.android.tools.idea.gradle.util.GradleUtil
import com.android.tools.klint.client.api.SdkWrapper
import org.jetbrains.android.facet.AndroidFacet
import kotlin.reflect.memberFunctions
import kotlin.reflect.staticFunctions

class AndroidModelFacade(val facet: AndroidFacet) {
    private val model by lazy { loadModel() }
    private val modelClass by lazy {
        try {
            Class.forName("com.android.tools.idea.gradle.AndroidGradleModel")
        }
        catch(e: ClassNotFoundException) {
            null
        }
    }

    fun getLocalSdk(): SdkWrapper? {
        val sdkData = facet.sdkData ?: return null
        val localSdk = sdkData.javaClass.kotlin.memberFunctions
                .firstOrNull { it.name == "getLocalSdk" || it.name == "getSdkHandler" }
                ?.call(sdkData) ?: return null
        return SdkWrapper(localSdk)
    }

    fun isModelReady() = model != null

    @Suppress("UNCHECKED_CAST")
    fun getFlavorSourceProviders(): List<SourceProvider>?
            = getFacetOrModel("getFlavorSourceProviders") as? List<SourceProvider>

    fun getMultiFlavorSourceProvider(): SourceProvider?
            = getFacetOrModel("getMultiFlavorSourceProvider") as? SourceProvider

    fun getBuildTypeSourceProvider(): SourceProvider?
            = getFacetOrModel("getBuildTypeSourceProvider") as? SourceProvider

    fun getVariantSourceProvider(): SourceProvider?
            = getFacetOrModel("getVariantSourceProvider") as? SourceProvider

    private fun getFacetOrModel(methodName: String): Any? {
        if (modelClass == null) {
            return facet.javaClass.kotlin.memberFunctions
                    .firstOrNull { it.name == methodName }
                    ?.call(facet)
        }

        val model = model ?: return null

        return modelClass?.kotlin?.memberFunctions
                ?.firstOrNull { it.name == methodName }
                ?.call(model)
    }

    fun getDependsOn(artifact: String): Boolean {
        val model = model ?: return false
        return GradleUtil::class.staticFunctions.firstOrNull {
            val type = it.parameters.firstOrNull()?.type?.toString() ?: ""
            it.parameters.size == 2 && "IdeaAndroidProject" in type || "AndroidGradleModel" in type
        }?.call(model, artifact) as? Boolean ?: false
    }

    fun getAndroidProject(): AndroidProject? {
        val model = model ?: return null
        return modelClass?.kotlin?.memberFunctions
                ?.firstOrNull { it.name == "getDelegate" || it.name == "getAndroidProject" }
                ?.call(model) as? AndroidProject
    }

    fun getSelectedVariant(): Variant? {
        val model = model ?: return null
        return modelClass?.kotlin?.memberFunctions
                ?.firstOrNull { it.name == "getSelectedVariant" }
                ?.call(model) as? Variant
    }

    private fun loadModel(): Any? {
        try {
            val getAndroidProjectInfoFun = AndroidFacet::class.memberFunctions.singleOrNull {
                it.name == "getIdeaAndroidProject" || it.name == "getAndroidModel"
            }

            return getAndroidProjectInfoFun?.call(facet)
        }
        catch(e: Throwable) {
            return false
        }
    }

    fun computePackageName(): String? {
        return getSelectedVariant()?.mainArtifact?.applicationId
    }

}