package com.example.obd_servise.command

import com.github.eltonvs.obd.command.ATCommand

class SetEchoOffCommand : ATCommand() {
    override val tag = "SET_ECHO_OFF"
    override val name = "Set Echo Off"
    override val pid = "E0"
}