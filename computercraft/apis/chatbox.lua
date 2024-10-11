-- Chatbox Lua API for ComputerCraft
-- mimicking https://github.com/SwitchCraftCC/sc3-rom/blob/1.20.1/switchcraft/data/computercraft/lua/rom/apis/lua
-- work in progress

local expect = loadfile("/rom/modules/main/cc/expect.lua")

local SERVER_URL = "wss://chat.reconnected.cc"

local closeReasons = {
    SERVER_STOPPING = 4000,
    EXTERNAL_GUESTS_NOT_ALLOWED = 4001,
    UNKNOWN_LICENSE_KEY = 4002,
    INVALID_LICENSE_KEY = 4003,
    DISABLED_LICENSE = 4004,
    CHANGED_LICENSE_KEY = 4005,
    FATAL_ERROR = 4100,
    UNSUPPORTED_ENDPOINT = 4101,
}

local running = false
local connected = false
local ws

-- from hello packet
local ownerName, owner, guest
local capabilities = {}
-- from players packet
local players = {}

local function getLicenseKey()
    return settings.get("chatbox.license_key"):gsub("%s", "")
end

function _shouldStart()
    return getLicenseKey() ~= nil
end

local function send(rawData)
    local data = textutils.serializeJSON(rawData)
    ws.send(data)
end

local function updatePlayer(player)
    if not guest and player.uuid == owner.uuid then
        owner = player
        ownerName = owner.name or owner.uuid
    end

    for k, v in pairs(players) do
        if v.uuid == player.uuid then
            players[k] = player
            return
        end
    end

    table.insert(players, player)
end

local function emitChatEvents(data)
    local event = data.event
    local pars = {}

    if event == "chat_ingame" then
        pars = { data.user.name or data.user.uuid }
    elseif event == "chat_discord" then
        local name
        if data.discordUser.name then
            if data.discordUser.discriminator == "0000" then
                name = data.discordUser.name
            else
                name = data.discoUser.name .. "#" .. data.discordUser.discriminator
            end
        else
            name = tostring(data.discordUser.id)
        end
        pars = { name }
    elseif event == "chat_chatbox" then
        -- to implement on the server
        pars = { data.rawName, data.user.name or data.user.uuid }
    end

    os.queueEvent(event, table.unpack(pars), data.rawText or data.text, data)
end

local function processData(rawData)
    local data = textutils.unserializeJSON(rawData)

    if not data or not data.type then
        return
    end

    if data.type == "hello" then
        guest = data.guest
        ownerName = data.licenseOwner
        owner = data.licenseOwnerUser
        capabilities = data.capabilities
        connected = true
    elseif data.type == "players" then
        players = data.players
    elseif data.type == "event" then
        local event = data.event
        if event:sub(1, 5) == "chat_" then
            emitChatEvents(data)
        elseif event == "command" then
            local username = data.user.name or data.user.uuid
            os.queueEvent("command", username, data.command, data.args, data)
        elseif event == "join" then
            updatePlayer(data.user)
            local username = data.user.name or data.user.uuid
            os.queueEvent("join", username, data)
        elseif event == "leave" then
            updatePlayer(data.user)
            local username = data.user.name or data.user.uuid
            os.queueEvent("leave", username, data)
        elseif event == "death" then
            updatePlayer(data.user)
            local username = data.user.name or data.user.uuid
            local source = data.source and (data.source.name or data.source.uuid) or nil
            os.queueEvent("death", username, source, data.text, data)
        elseif event == "world_change" then
            updatePlayer(data.user)
            local username = data.user.name or data.user.uuid
            os.queueEvent("world_change", username, data.origin, data.destination, data)
        end
    end
end

function say(text, name, mode)
    if not isConnected() or not ws then
        error("Chatbox not connected", 2)
    end
    if not hasCapability("say") then
        error("You do not have the 'say' capability", 2)
    end
    expect(text, 1, "string")
    expect(name, 2, "string", "nil")
    expect(mode, 3, "string", "nil")

    if mode and mode ~= "format" and mode ~= "markdown" then
        error("Invalid mode. Must be either 'markdown' or 'format'", 2)
    end

    send({
        type = "say",
        text = text,
        name = name,
        mode = mode or "markdown"
    })
end

function tell(user, text, name, mode)
    if not isConnected() or not ws then
        error("Chatbox not connected", 2)
    end
    if not hasCapability("tell") then
        error("You do not have the 'tell' capability", 2)
    end
    expect(text, 1, "string")
    expect(text, 2, "string")
    expect(name, 3, "string", "nil")
    expect(mode, 4, "string", "nil")

    if mode and mode ~= "format" and mode ~= "markdown" then
        error("Invalid mode. Must be either 'markdown' or 'format'", 2)
    end

    send({
        type = "tell",
        user = user,
        text = text,
        name = name,
        mode = mode or "markdown"
    })
end

function stop()
    running = false
    if ws and ws.close then
        pcall(ws.close)
    end
end

function run()
    if running then
        error("Chatbox already running", 2)
    end
    running = true

    local licenseKey = getLicenseKey()
    local wsEndpoint = SERVER_URL .. "/v2/" .. textutils.urlEncode(licenseKey)

    http.websocketAsync(wsEndpoint)

    while running do
        local ev = { os.pullEventRaw() }

        if ev[1] == "websocket_success" and ev[2] == wsEndpoint then
            ws = ev[3]
        elseif ev[1] == "websocket_message" and ev[2] == wsEndpoint then
            processData(ev[3])
        elseif ev[1] == "websocket_closed" and ev[2] == wsEndpoint then
            running = false
        end
    end
end

function getError()

end

function isConnected()
    return running and connected
end

function getLicenseOwner()
    return ownerName, owner
end

function getCapabilities()
    return capabilities or {}
end

function getPlayers()
    return players or {}
end

function getPlayerList()
    local playerList = {}
    for i, player in pairs(players or {}) do
        table.insert(playerList, player.name or player.uuid)
    end
    return playerList
end

function hasCapability(capability)
    capability = capability:lower()
    for i, v in ipairs(capabilities) do
        if v:lower() == capability then
            return true
        end
    end
    return false
end

function isGuest()
    return guest
end