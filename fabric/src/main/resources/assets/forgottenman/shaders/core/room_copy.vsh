#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float GameTime;
uniform float Desync;
uniform float Distortion;

out vec4 vertexColor;
out vec2 texCoord0;
out float viewDist;

void main() {
    vec3 pos = Position;
    // Reality-break wobble: each copy breathes on its own desynced clock.
    float t = GameTime * 1200.0 + Desync;
    pos += Distortion * 0.08 * vec3(
        sin(t + Position.y * 0.9 + Position.x * 0.31),
        0.4 * sin(t * 0.7 + Position.z * 0.53),
        cos(t * 0.85 + Position.y * 0.47));

    vec4 view = ModelViewMat * vec4(pos, 1.0);
    viewDist = length(view.xyz);
    gl_Position = ProjMat * view;

    vertexColor = Color;
    texCoord0 = UV0;
}
