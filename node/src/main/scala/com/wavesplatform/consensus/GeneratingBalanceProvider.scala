package com.wavesplatform.consensus

import com.wavesplatform.account.Address
import com.wavesplatform.block.Block
import com.wavesplatform.block.Block.BlockId
import com.wavesplatform.common.state.ByteStr
import com.wavesplatform.features.BlockchainFeatures
import com.wavesplatform.state.Blockchain

object GeneratingBalanceProvider {
  private val MinimalEffectiveBalanceForGenerator1: Long = 1000000000000L
  private val MinimalEffectiveBalanceForGenerator2: Long = 100000000000L
  private val FirstDepth                                 = 50
  private val SecondDepth                                = 1000

  def minimalEffectiveBalance(height: Int, activatedFeatures: Map[Short, Int]): Long = {
    val activated = activatedFeatures.get(BlockchainFeatures.SmallerMinimalGeneratingBalance.id).exists(height >= _)
    if (activated) MinimalEffectiveBalanceForGenerator2
    else MinimalEffectiveBalanceForGenerator1
  }

  def minimalBlockInterval(height: Int, activatedFeatures: Map[Short, Int]): Int = {
    val activated = activatedFeatures.get(BlockchainFeatures.SmallerMinimalGeneratingBalance.id).exists(height >= _)
    if (activated) SecondDepth
    else FirstDepth
  }

  def isMiningAllowed(blockchain: Blockchain, height: Int, effectiveBalance: Long): Boolean =
    effectiveBalance >= minimalEffectiveBalance(blockchain.height, blockchain.activatedFeatures)

  //noinspection ScalaStyle
  def isEffectiveBalanceValid(blockchain: Blockchain, height: Int, block: Block, effectiveBalance: Long): Boolean =
    block.timestamp < blockchain.settings.functionalitySettings.minimalGeneratingBalanceAfter || (block.timestamp >= blockchain.settings.functionalitySettings.minimalGeneratingBalanceAfter && effectiveBalance >= MinimalEffectiveBalanceForGenerator1) ||
      blockchain.activatedFeatures
        .get(BlockchainFeatures.SmallerMinimalGeneratingBalance.id)
        .exists(height >= _) && effectiveBalance >= MinimalEffectiveBalanceForGenerator2

  def balance(blockchain: Blockchain, account: Address, blockId: BlockId = ByteStr.empty): Long = {
    val height =
      if (blockId.isEmpty) blockchain.height
      else blockchain.heightOf(blockId).getOrElse(throw new IllegalArgumentException(s"Invalid block ref: $blockId"))

    val depth = if (height >= blockchain.settings.functionalitySettings.generationBalanceDepthFrom50To1000AfterHeight) SecondDepth else FirstDepth
    blockchain.effectiveBalance(account, depth, blockId)
  }
}
