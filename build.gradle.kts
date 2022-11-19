import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerLogsContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStopContainer
import com.bmuschko.gradle.docker.tasks.image.DockerPullImage

plugins {
    id("com.bmuschko.docker-remote-api") version "9.0.1"
}

val pullJekyllImage by tasks.creating(DockerPullImage::class) {
    image.set("ghcr.io/marcphilipp/gh-pages-jekyll:latest")
}

tasks.withType<DockerCreateContainer>().configureEach {
    dependsOn(pullJekyllImage)
    targetImageId(pullJekyllImage.image)
    hostConfig.binds.put(file("jekyll").absolutePath, "/workspace")
}

val createJekyllServerContainer by tasks.creating(DockerCreateContainer::class) {
    containerName.set("gh-pages-jekyll-server")
    hostConfig.portBindings.add("4000:4000")
    hostConfig.autoRemove.set(true)
    cmd.addAll("serve", "--watch", "--host", "0.0.0.0")
}

val startJekyllServerContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createJekyllServerContainer)
    targetContainerId(createJekyllServerContainer.containerId)
}

tasks.withType<DockerLogsContainer>().configureEach {
    tailAll.set(true)
    follow.set(true)
}

val followJekyllServerContainer by tasks.creating(DockerLogsContainer::class) {
    dependsOn(startJekyllServerContainer)
    targetContainerId(startJekyllServerContainer.containerId)
}

val stopJekyllServerContainer by tasks.creating(DockerStopContainer::class) {
    targetContainerId(createJekyllServerContainer.containerName)
}

tasks.register("serveJekyllSite") {
    dependsOn(followJekyllServerContainer)
}

val createJekyllBuilderContainer by tasks.creating(DockerCreateContainer::class) {
    containerName.set("gh-pages-jekyll-builder")
    cmd.add("build")
}

val startJekyllBuilderContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(createJekyllBuilderContainer)
    targetContainerId(createJekyllBuilderContainer.containerId)
}

val followJekyllBuilderContainer by tasks.creating(DockerLogsContainer::class) {
    dependsOn(startJekyllBuilderContainer)
    targetContainerId(startJekyllBuilderContainer.containerId)
}

val removeJekyllBuilderContainer by tasks.creating(DockerRemoveContainer::class) {
    targetContainerId(createJekyllBuilderContainer.containerName)
}

tasks.register("generateJekyllSite") {
    dependsOn(followJekyllBuilderContainer)
    finalizedBy(removeJekyllBuilderContainer)
}
