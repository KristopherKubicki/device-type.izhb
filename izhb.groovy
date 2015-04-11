/**
 *  Improved Zigbee Hue Bulb
 *
 *   Removed the color adjuster.  Added parsers for level and hue
 *  
 *
 *  Copyright 2015 SmartThings
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

metadata {
	// Automatically generated. Make future change here.
	definition (name: "Improved Zigbee Hue Bulb", namespace: "smartthings", author: "SmartThings") {
		capability "Switch Level"
		capability "Actuator"
		capability "Color Control"
		capability "Switch"
		capability "Configuration"
		capability "Polling"
		capability "Refresh"
		capability "Sensor"
       
        command "setAdjustedColor"
        
        // This is a new temporary counter to keep track of no responses
        attribute "unreachable", "number"

		fingerprint profileId: "C05E", inClusters: "0000,0003,0004,0005,0006,0008,0300,1000", outClusters: "0019"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	// UI tile definitions
	tiles {
		standardTile("switch", "device.switch", width: 1, height: 1, canChangeIcon: true) {
			state "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff"
			state "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#79b821"
		}
		standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat") {
			state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
		}
		controlTile("rgbSelector", "device.color", "color", height: 3, width: 3, inactiveLabel: false) {
			state "color", action:"setAdjustedColor"
		}
		controlTile("levelSliderControl", "device.level", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "level", action:"switch level.setLevel"
		}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat") {
			state "level", label: 'Level ${currentValue}%'
		}
		controlTile("saturationSliderControl", "device.saturation", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "saturation", action:"color control.setSaturation"
		}
		valueTile("saturation", "device.saturation", inactiveLabel: false, decoration: "flat") {
			state "saturation", label: 'Sat ${currentValue}    '
		}
		controlTile("hueSliderControl", "device.hue", "slider", height: 1, width: 2, inactiveLabel: false) {
			state "hue", action:"color control.setHue"
		}

		main(["switch"])
		details(["switch", "levelSliderControl", "rgbSelector", "refresh"])
	}
}

// Parse incoming device messages to generate events
def parse(String description) {
	log.trace description
    
    sendEvent(name: 'unreachable', value: 0)
    
    if (description?.startsWith("catchall:")) {
        if(description?.endsWith(" 0100"))
        {
        	def result = createEvent(name: "switch", value: "on")
            log.debug "Parse returned ${result?.descriptionText}"
            return result
        }
        if(description?.startsWith("0104 0006") && description?.endsWith(" 0000"))
        {
        	def result = createEvent(name: "switch", value: "off")
            log.debug "Parse returned ${result?.descriptionText}"
            return result
        }

	}
    if (description?.startsWith("read attr")) {
    
        Map descMap = (description - "read attr - ").split(",").inject([:]) { map, param ->
		def nameAndValue = param.split(":")
		map += [(nameAndValue[0].trim()):nameAndValue[1].trim()]
		}
		
        switch (descMap.cluster) {
            
        	case "0008":
        		def i = Math.round(convertHexToInt(descMap.value) / 255 * 100 )
                def result = createEvent( name: "level", value: i)
                return result
             
             // This doesn't seem to be updating the saturation or the hue in the app, even though it should
         	case "0300":           		
                def i = Math.round(convertHexToInt(descMap.value) / 255 * 100 )
                if(descMap.attrId == "0000") { 
                    def smallHex = hsvToHex(i / 100,device.currentValue("saturation") / 100)
                    if(device.currentValue("hex") != smallHex) { 
    					sendEvent(name: "color", value: smallHex)
                    }
                    def result = createEvent( name: "hue", value: i)
                	return result
                }
                if(descMap.attrId == "0001") {  
                	def smallHex = hsvToHex(device.currentValue("hue") / 100,i / 100)
                   	if(device.currentValue("hex") != smallHex) { 
    					sendEvent(name: "color", value: smallHex)
                    }
                    def result = createEvent( name: "saturation", value: i)
                    return result
                }
    	}                       
    }

}

def on() {
	// just assume it works for now
	log.debug "on()"
	sendEvent(name: "switch", value: "on")
	"st cmd 0x${device.deviceNetworkId} ${endpointId} 6 1 {}"
}

def off() {
	// just assume it works for now
	log.debug "off()"
	sendEvent(name: "switch", value: "off")
	"st cmd 0x${device.deviceNetworkId} ${endpointId} 6 0 {}"
}

def setHue(value) {
	def max = 0xfe
//	log.trace "setHue($value)"
	sendEvent(name: "hue", value: value)
	def scaledValue = Math.round(value * max / 100.0)
	def cmd = "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x300 0x00 {${hex(scaledValue)} 00 0000}"
	//log.info cmd
	cmd
}

def setAdjustedColor(value){
	value.level = device.currentValue("level")
    setColor(value)
}

def setColor(value){
	log.trace "setColor($value)"
    
    def unreachable = device.latestValue("unreachable")
    if(unreachable == null) { 
    	sendEvent(name: 'unreachable', value: 1)
    }
    else { 
    	sendEvent(name: 'unreachable', value: unreachable + 1)
    }
    if(unreachable > 2) { 
    	sendEvent(name: "switch", value: "off")
    }
    
	def max = 0xfe
	value.hue = value.hue as Integer
    value.saturation = value.saturation as Integer	

	def smallHex = hsvToHex(value.hue / 100,value.saturation / 100)
    
    sendEvent(name: "color", value: smallHex)
	sendEvent(name: "hue", value: value.hue)
	sendEvent(name: "saturation", value: value.saturation)
	def scaledHueValue = Math.round(value.hue * max / 100.0)
	def scaledSatValue = Math.round(value.saturation * max / 100.0)

	def cmd = []
	if (value.switch != "off" && device.currentValue("switch") == "off") {
		cmd << "st cmd 0x${device.deviceNetworkId} ${endpointId} 6 1 {}"
		cmd << "delay 150"
	}

	cmd << "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x300 0x00 {${hex(scaledHueValue)} 00 0000}"
	cmd << "delay 150"
	cmd << "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x300 0x03 {${hex(scaledSatValue)} 0000}"

	if (value.level != null && value.level * 100 != device.currentValue("level")) {
		cmd << "delay 150"
		cmd.addAll(setLevel(value.level))
	}

	if (value.switch == "off") {
		cmd << "delay 150"
		cmd << off()
	}
	log.info cmd
	cmd
}

def setSaturation(value) {

	def max = 0xfe
	log.trace "setSaturation($value)"
	sendEvent(name: "saturation", value: value)
	def scaledValue = Math.round(value * max / 100.0)
	def cmd = "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x300 0x03 {${hex(scaledValue)} 0000}"
	//log.info cmd
	cmd
}

def refresh() {

    def unreachable = device.latestValue("unreachable")
    if(unreachable == null) { 
    	sendEvent(name: 'unreachable', value: 1)
    }
    else { 
    	sendEvent(name: 'unreachable', value: unreachable + 1)
    }
    if(unreachable > 2) { 
    	sendEvent(name: "switch", value: "off")
    }
    
// Ping the device with color as to not get out of sync 
    [
	"st rattr 0x${device.deviceNetworkId} ${endpointId} 6 0", "delay 500",
    "st rattr 0x${device.deviceNetworkId} ${endpointId} 8 0", "delay 500",
    "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0300 1","delay 500",
    "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0300 0"
    ]
}

def poll(){
	log.debug "Poll is calling refresh"
	refresh()
}

def setLevel(value) {
	log.trace "setLevel($value)"
    
    def unreachable = device.latestValue("unreachable")
    if(unreachable == null) { 
    	sendEvent(name: 'unreachable', value: 1)
    }
    else { 
    	sendEvent(name: 'unreachable', value: unreachable + 1)
    }
    if(unreachable > 2) { 
    	sendEvent(name: "switch", value: "off")
    }
    
	def cmds = []

	if (value == 0) {
		sendEvent(name: "switch", value: "off")
		cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 6 0 {}"
	}
	else if (device.latestValue("switch") == "off" && unreachable < 2) {
		sendEvent(name: "switch", value: "on")
	}

	sendEvent(name: "level", value: value)
	def level = hex(value * 2.55)
    if(value == 1) { level = hex(1) }
	cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 8 4 {${level} 0000}"

	//log.debug cmds
	cmds
}

private getEndpointId() {
	new BigInteger(device.endpointId, 16).toString()
}

private hex(value, width=2) {
	def s = new BigInteger(Math.round(value).toString()).toString(16)
	while (s.size() < width) {
		s = "0" + s
	}
	s
}

private Integer convertHexToInt(hex) {
	Integer.parseInt(hex,16)
}


// Derived from java.awt.Color
def hsvToHex(float hue, float saturation) {

    int h = (int)(hue * 6);
    float f = hue * 6 - h;
    float p = (1 - saturation);
    float q = (1 - f * saturation);
    float t = (1 - (1 - f) * saturation);

    switch (h) {
      case 0: return rgbToString(1, t, p);
      case 1: return rgbToString(q, 1, p);
      case 2: return rgbToString(p, 1, t);
      case 3: return rgbToString(p, q, 1);
      case 4: return rgbToString(t, p, 1);
      case 5: return rgbToString(1, p, q);
    }
}

def rgbToString(float r, float g, float b) {
    String rs = Integer.toHexString((int)(r * 255))
    String gs = Integer.toHexString((int)(g * 255));
    String bs = Integer.toHexString((int)(b * 255));
    return "#" + rs.toUpperCase() + gs.toUpperCase() + bs.toUpperCase();
}
