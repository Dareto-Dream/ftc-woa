import pygame
import json
import math
import os
from pygame.locals import *

# -----------------------------------------
# CONFIG
# -----------------------------------------
FIELD_WIDTH_IN = 144
FIELD_HEIGHT_IN = 144
BACKGROUND_IMAGE = "field.png"

OUTPUT_FILE = "path.json"

WINDOW_SCALE = 6
WINDOW_W = FIELD_WIDTH_IN * WINDOW_SCALE
WINDOW_H = FIELD_HEIGHT_IN * WINDOW_SCALE

STEP_SIZE = 0.5

GRID_INCHES = 24

# Snapping
SNAP_ENABLED = False
SNAP_INCHES = 24
MIN_SNAP = 0

# Snapping Offsets
GRID_OFFSET = 0         # inches (default no offset)
GRID_OFFSET_MODE = False

# -----------------------------------------
pygame.init()
screen = pygame.display.set_mode((WINDOW_W, WINDOW_H))
pygame.display.set_caption("Path Tracer")

clock = pygame.time.Clock()
font = pygame.font.SysFont("consolas", 20)

if os.path.exists(BACKGROUND_IMAGE):
    bg = pygame.image.load(BACKGROUND_IMAGE).convert()
    bg = pygame.transform.scale(bg, (WINDOW_W, WINDOW_H))
else:
    bg = None

drawing = False
last_point = None
path_points = []

SNAP_ANGLES = [math.radians(a) for a in [0, 45, 90, 135, 180, 225, 270, 315]]

def px_to_inch(px, py):
    return px / WINDOW_SCALE, py / WINDOW_SCALE

# -----------------------------------------
# SNAP COORDINATES
# -----------------------------------------
def snap_coord(x, y):
    if not SNAP_ENABLED:
        return x, y

    # Apply offset BEFORE rounding
    x_adj = x - GRID_OFFSET
    y_adj = y - GRID_OFFSET

    sx = round(x_adj / SNAP_INCHES) * SNAP_INCHES + GRID_OFFSET
    sy = round(y_adj / SNAP_INCHES) * SNAP_INCHES + GRID_OFFSET

    return sx, sy

# -----------------------------------------
def draw_grid():
    spacing_px = GRID_INCHES * WINDOW_SCALE
    offset_px = GRID_OFFSET * WINDOW_SCALE
    color = (50, 50, 50)

    # Vertical lines with offset
    x = offset_px % spacing_px
    while x <= WINDOW_W:
        pygame.draw.line(screen, color, (x, 0), (x, WINDOW_H), 1)
        x += spacing_px

    # Horizontal lines with offset
    y = offset_px % spacing_px
    while y <= WINDOW_H:
        pygame.draw.line(screen, color, (0, y), (WINDOW_W, y), 1)
        y += spacing_px

# -----------------------------------------
def draw_path():
    if len(path_points) < 2:
        return

    for i in range(len(path_points) - 1):
        x1, y1 = path_points[i]
        x2, y2 = path_points[i + 1]

        pygame.draw.line(
            screen,
            (255, 0, 0),
            (x1 * WINDOW_SCALE, y1 * WINDOW_SCALE),
            (x2 * WINDOW_SCALE, y2 * WINDOW_SCALE),
            3
        )

# -----------------------------------------
def save_path():
    data = {"path": [{"x": x, "y": y} for x, y in path_points]}
    with open(OUTPUT_FILE, "w") as f:
        json.dump(data, f, indent=2)
    print("Saved:", OUTPUT_FILE)

# -----------------------------------------
# UI overlay
# -----------------------------------------
def draw_ui():
    text = f"Snap: {'ON' if SNAP_ENABLED else 'OFF'}  |  Snap Size: {SNAP_INCHES} in"
    surf = font.render(text, True, (255, 255, 255))
    screen.blit(surf, (10, 10))

# -----------------------------------------
running = True
while running:
    screen.fill((30, 30, 30))

    if bg:
        screen.blit(bg, (0, 0))

    draw_grid()
    draw_path()
    draw_ui()

    for event in pygame.event.get():
        if event.type == QUIT:
            running = False

        if event.type == KEYDOWN:
            if event.key == K_ESCAPE:
                running = False

            if event.key == K_s:
                save_path()

            if event.key == K_c:
                path_points.clear()
                last_point = None
                print("Cleared path")

            # --------------------------
            # Toggle snapping
            # --------------------------
            if event.key == K_g:
                SNAP_ENABLED = not SNAP_ENABLED
                print("Grid snapping:", SNAP_ENABLED)

            # --------------------------
            # Toggle grid offset-edit mode
            # --------------------------
            if event.key == K_BACKQUOTE:   # key: `
                GRID_OFFSET_MODE = not GRID_OFFSET_MODE
                print("Grid offset mode:", GRID_OFFSET_MODE)

            # --------------------------
            # Increase snap (+4)
            # --------------------------
            if event.key == K_EQUALS or event.key == K_PLUS:
                if GRID_OFFSET_MODE:
                    GRID_OFFSET += 4
                    print("Grid offset:", GRID_OFFSET)
                else:
                    SNAP_INCHES += 4
                    print("Snap size:", SNAP_INCHES)

            # --------------------------
            # Decrease snap (-4)
            # --------------------------
            if event.key == K_MINUS:
                if GRID_OFFSET_MODE:
                    GRID_OFFSET -= 4
                    print("Grid offset:", GRID_OFFSET)
                else:
                    SNAP_INCHES = max(MIN_SNAP, SNAP_INCHES - 4)
                    print("Snap size:", SNAP_INCHES)

        # --------------------------
        # Start drawing
        # --------------------------
        if event.type == MOUSEBUTTONDOWN and event.button == 1:
            drawing = True
            mx, my = pygame.mouse.get_pos()
            xin, yin = px_to_inch(mx, my)
            xin, yin = snap_coord(xin, yin)  # <-- snap here
            last_point = (xin, yin)
            path_points.append(last_point)

        # --------------------------
        # Stop drawing
        # --------------------------
        if event.type == MOUSEBUTTONUP and event.button == 1:
            drawing = False
            last_point = None

    # --------------------------
    # Draw step-by-step segments
    # --------------------------
    if drawing and last_point:
        mx, my = pygame.mouse.get_pos()
        mx_in, my_in = px_to_inch(mx, my)
        mx_in, my_in = snap_coord(mx_in, my_in)

        lx, ly = last_point
        dx = mx_in - lx
        dy = my_in - ly

        if abs(dx) > 0.05 or abs(dy) > 0.05:
            angle = math.atan2(dy, dx)
            best = min(SNAP_ANGLES, key=lambda a: abs(a - angle))

            new_x = lx + math.cos(best) * STEP_SIZE
            new_y = ly + math.sin(best) * STEP_SIZE

            new_x, new_y = snap_coord(new_x, new_y)  # snap each step

            last_point = (new_x, new_y)
            path_points.append(last_point)

    pygame.display.flip()
    clock.tick(60)

pygame.quit()
