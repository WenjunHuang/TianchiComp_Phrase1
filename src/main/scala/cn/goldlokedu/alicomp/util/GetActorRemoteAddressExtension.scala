package cn.goldlokedu.alicomp.util

import akka.actor.{ActorPath, Address, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, RootActorPath}

class GetActorRemoteAddressExtension(system: ExtendedActorSystem) extends Extension {
  def remotePath(local:ActorPath) = RootActorPath(system.provider.getDefaultAddress) / local.elements
}

object GetActorRemoteAddressExtension extends ExtensionId[GetActorRemoteAddressExtension] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): GetActorRemoteAddressExtension =
    new GetActorRemoteAddressExtension(system)

  override def lookup() = GetActorRemoteAddressExtension
}
