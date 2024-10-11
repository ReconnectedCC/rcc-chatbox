local args = { ... }

local function printUsage()
    print("Usage:")
    print("  chatbox register <license>")
    print("  chatbox register guest")
    print("  chatbox remove")
    print("Obtain a license with /chatbox")
end

if #args == 0 then
    printUsage()
    return
end

local command = args[1]

if command == "register" then
    local license = args[2]
    if not license then
        printUsage()
        return
    end

    license = license:gsub("%s+", "")
    settings.set("chatbox.license_key", license)
    settings.save(".settings")

    print("Success!")
    print("Your chatbox license key has been changed.")
    print("A reboot is required for the changes to take effect.")

    print()
    write("Do you want to reboot now? [y/N] ")
    local ans = read()
    if ans:lower():gsub("%s+", "") == "y" then
        os.reboot()
    end

elseif command == "remove" then
    settings.unset("chatbox.license_key")
    settings.save(".settings")
    if chatbox then
        chatbox.stop()
    end
else
    printUsage()
end
