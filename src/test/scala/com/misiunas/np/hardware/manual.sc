import akka.actor.ActorSystem
import com.misiunas.np.hardware.adc.control.{DAC, DACStatus, DepositionElectrode, ImagingElectrode}
import com.misiunas.np.hardware.adc.control.DAC.SetDC_V
import com.misiunas.np.hardware.communication.CommunicationTCP$
import com.misiunas.np.tools.Talkative

val system = ActorSystem("3DnP")

val tcp = system.actorOf(CommunicationTCP.propsForADCControls(), "tcp")

val dac = system.actorOf(DAC.props(), "dac")

dac ! DAC.SetDC_V(100)

dac ! DAC.SetMode(DepositionElectrode)


DACStatus( 0, 0, ac_frequency = 50, false, ImagingElectrode )