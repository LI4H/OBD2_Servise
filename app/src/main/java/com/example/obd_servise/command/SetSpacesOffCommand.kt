package com.example.obd_servise.command


import com.github.eltonvs.obd.command.ATCommand

class SetSpacesOffCommand : ATCommand() {
    override val tag = "SET_SPACES_OFF"
    override val name = "Set Spaces Off"
    override val pid = "S0"
}