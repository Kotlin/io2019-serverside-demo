import com.google.cloud.datastore.DatastoreOptions
import com.google.cloud.datastore.Entity
import com.google.cloud.vision.v1.*
import com.google.datastore.v1.Key
import com.google.gson.Gson
import com.google.gson.JsonObject

class VisionFunction {
    private val imageAnnotatorClient = ImageAnnotatorClient.create()
    private val datastore = DatastoreOptions.newBuilder().build().service

    data class GCSEvent(
            var bucket : String? = null,
            var name : String ? = null
    )

    fun applyVisionLabels(event : GCSEvent) {
        println(event)

        val response = imageAnnotatorClient.batchAnnotateImages(listOf(
                AnnotateImageRequest.newBuilder()
                        .setImage(Image.newBuilder()
                                .setSource(ImageSource.newBuilder()
                                        .setGcsImageUri("gs://${event.bucket}/${event.name}")))
                        .addFeatures(Feature.newBuilder()
                                .setType(Feature.Type.LABEL_DETECTION)
                                .setMaxResults(3))
                        .build()
        ))

        println(response)

        val labels = response.getResponses(0)
                .labelAnnotationsList
                .sortedByDescending { it.score }
                .map { it.description }

        println(labels)

        val keyFactory = datastore.newKeyFactory()
        val id = event.name!!.split("/")[1]
        val key = keyFactory.setKind("photo").newKey(id)
        val entity = datastore.get(key)
        datastore.update(Entity.newBuilder(entity).set("label", labels.joinToString(",")).build())
    }
}

