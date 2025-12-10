//
// Created by Gabriele.Pappalardo on 25/07/2025.
//

#ifndef HOTRELOADSERVER_HPP
#define HOTRELOADSERVER_HPP

#ifdef KONAN_HOT_RELOAD

#include "CompilerConstants.hpp"

#include <iostream>
#include <vector>
#include <string>
#include <thread>
#include <string_view>

#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include "HotReloadUtility.hpp"

using namespace kotlin::hot;

class HotReloadServer {
public:
    explicit HotReloadServer(
            const int32_t port = (kotlin::compiler::hotReloadServerPort() == -1) ? kDefaultServerPort
                                                                             : kotlin::compiler::hotReloadServerPort()) :
        port(port) {}

    ~HotReloadServer() { stop(); }

    bool start() {
        // Create socket
        serverFd = socket(AF_INET, SOCK_STREAM, 0);
        if (serverFd == -1) {
            HRLogError("(HotReloadServer) Failed to create TCP socket for reload requests");
            return false;
        }

        // Set socket options to reuse address
        int opt = 1;
        if (setsockopt(serverFd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt)) < 0) {
            HRLogError("(HotReloadServer) Failed to set socket options");
            close(serverFd);
            return false;
        }

        // Bind to localhost
        sockaddr_in address;
        address.sin_family = AF_INET;
        address.sin_addr.s_addr = inet_addr(kServerEndpoint);
        address.sin_port = htons(port);

        if (bind(serverFd, reinterpret_cast<sockaddr*>(&address), sizeof(address)) < 0) {
            HRLogError("(HotReloadServer) Failed to bind to port %s:%d", kServerEndpoint, port);
            close(serverFd);
            return false;
        }

        // Listen for connections
        if (listen(serverFd, 3) < 0) {
            HRLogError("(HotReloadServer) Failed to listen on socket");
            close(serverFd);
            return false;
        }

        running = true;
        HRLogInfo("(HotReloadServer) Listening on %s:%d", kServerEndpoint, port);
        return true;
    }

    template <typename F>
    void run(F&& onReloadMessageCallback) {
        /// We can only handle one request at time (for the moment).

        runningThread = std::make_unique<std::thread>([this, onReloadMessageCallback]() {
#if KONAN_APPLE
            pthread_setname_np("HotReload Server Listener Thread");
#endif

            if (!running) {
                HRLogError("(HotReloadServer) Server not started!");
                return;
            }

            sockaddr_in clientAddress;
            socklen_t clientLen = sizeof(clientAddress);

            while (running) {
                const int clientSocket = accept(serverFd, reinterpret_cast<struct sockaddr*>(&clientAddress), &clientLen);
                if (clientSocket < 0) {
                    if (running) {
                        HRLogError("(HotReloadServer) Failed to accept connection, moving to next one...");
                    }
                    continue;
                }

                HRLogDebug("(HotReloadServer) Accepting incoming client");
                handleReloadMessage(clientSocket, onReloadMessageCallback);
                close(clientSocket);
            }
        });
    }

    void stop() {
        if (running) {
            running = false;
            if (serverFd != -1) {
                close(serverFd);
                serverFd = -1;
            }
            HRLogInfo("(HotReloadServer) Stopping server...");
            if (runningThread != nullptr) {
                runningThread->join();
            }
        }
    }

private:
    static constexpr auto kServerEndpoint = "127.0.0.1";
    static constexpr auto kDefaultServerPort = 5567;

    int serverFd{0};
    int32_t port{0};
    bool running{false};
    std::unique_ptr<std::thread> runningThread{nullptr};

    static bool readExact(const int socket, void* buffer, const size_t bytes) {
        size_t totalRead = 0;
        char* buf = static_cast<char*>(buffer);

        while (totalRead < bytes) {
            const ssize_t readBytes = recv(socket, buf + totalRead, bytes - totalRead, 0);
            if (readBytes <= 0) {
                return false; // Connection closed or error
            }
            totalRead += readBytes;
        }
        return true;
    }

    template <typename F>
    static void handleReloadMessage(const int clientSocket, F&& onReloadCallback) {
        uint32_t numDylibs{0};
        if (!readExact(clientSocket, &numDylibs, sizeof(numDylibs))) {
            HRLogError("(HotReloadServer) Failed to read number of dylibs");
            return;
        }

        // Read each dylib path
        std::vector<std::string> dylibPaths;
        for (uint32_t i = 0; i < numDylibs; i++) {
            // Read path length
            uint32_t pathLength;
            if (!readExact(clientSocket, &pathLength, sizeof(pathLength))) {
                HRLogError("(HotReloadServer) Failed to read path length for dylib %u", i);
                return;
            }

            // Read path string
            std::vector<char> pathBuffer(pathLength);
            if (!readExact(clientSocket, pathBuffer.data(), pathLength)) {
                HRLogError("Failed to read path for dylib %u", i);
                return;
            }

            std::string path(pathBuffer.data(), pathLength);
            // Remove null terminator if present
            if (!path.empty() && path.back() == '\0') {
                path.pop_back();
            }

            dylibPaths.push_back(path);
        }

        onReloadCallback(dylibPaths);
    }
};

#endif


#endif // HOTRELOADSERVER_HPP