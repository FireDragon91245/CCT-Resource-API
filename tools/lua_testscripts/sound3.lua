local info = resourceapi.getSoundInfo("minecraft:ui.toast.challenge_complete", "gd")
print(textutils.serialise(info.sounds))
local sound = info.data["ui/toast/challenge_complete"]
print(#sound.byteData)
local speaker_data = resourceapi.soundBytesToSpeakerData(sound.byteData)
print("finished")
local speaker = peripheral.wrap("right")

-- speaker.playAudio(speaker_data)

local curr_index = 1
local CHUNK_SIZE = 16 * 1024
while curr_index <= #speaker_data do
    local chunk = {}
    for i = curr_index, math.min(curr_index + CHUNK_SIZE - 1, #speaker_data) do
        table.insert(chunk, speaker_data[i])
    end
    while not speaker.playAudio(chunk) do
        os.pullEvent("speaker_audio_empty")
    end
    curr_index = curr_index + CHUNK_SIZE
end

