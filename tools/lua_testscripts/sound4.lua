local info = resourceapi.getSoundInfo("minecraft:ui.toast.challenge_complete", "gs")
local audioStream = info.stream["ui/toast/challenge_complete"].createPcmSigned8SampleStream()

local speaker = peripheral.wrap("right")

local read = 0;
local data = {}
repeat
    read, data = audioStream.read(16 * 1024, data)
    if data ~= nil then 
        while not speaker.playAudio(data) do
            os.pullEvent("speaker_audio_empty")
        end
    end
until read == 0
audioStream.close()

