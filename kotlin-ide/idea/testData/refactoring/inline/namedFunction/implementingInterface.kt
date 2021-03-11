// ERROR: Inlined function overrides function Contract.oblige

interface Contract {
    fun oblige(): Int
}

class ContractImpl : Contract {
    override fun oblige() = 0
}

class CallContract(pc: Contract, pci: ContractImpl) {
    val vci = pci.ob<caret>lige()
}